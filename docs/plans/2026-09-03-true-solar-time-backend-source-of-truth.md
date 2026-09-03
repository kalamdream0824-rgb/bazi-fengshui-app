# True Solar Time Backend Source of Truth Implementation Plan

> **For Claude:** Use `${SUPERPOWERS_SKILLS_ROOT}/skills/collaboration/executing-plans/SKILL.md` to implement this plan task-by-task.

**Goal:** Make the backend the single source of truth for birth-place resolution and true-solar-time correction so chart, saved record, report generation, and checkout always use the same corrected birth instant.

**Architecture:** Keep `PaipanRequest.solarDateTime` as the civil clock time entered by the user and keep `birthPlace`/`trueSolarTime` backward compatible. Add a deterministic backend resolver that maps the selected city to longitude, applies the NOAA equation-of-time formula for UTC+8, returns both the effective `LocalDateTime` and audit metadata, and runs before every call to `lunar-java`. The HTTP frontend stops pre-adjusting requests; mock mode may retain the TypeScript implementation but must pass shared golden vectors.

**Tech Stack:** Java 17, Spring Boot 3.5, lunar-java 1.7.4, Jackson, JUnit 5, React 19, TypeScript 6, Vitest, OpenAPI 3.0.

---

## Product and calculation rules

1. `solarDateTime` always means the original civil clock time entered by the user; it is never overwritten in stored request JSON.
2. When `trueSolarTime=false`, the effective time equals the original time and `PaipanResult.trueSolar` is `null`.
3. When `trueSolarTime=true`, a complete supported city is mandatory. Unknown or province-only locations fail closed with `BIRTH_PLACE_UNRESOLVED`; the backend must not silently use a provincial-capital longitude. For Beijing, Tianjin, Shanghai, and Chongqing, the current selector returns a district as the second-level value while the coordinate set is city-level, so these four municipalities explicitly resolve to their municipality-center longitude.
4. The first release supports the existing China-region selector and UTC+8 only. Foreign time zones and historical daylight-saving adjustments are explicitly out of scope and must be disclosed rather than guessed.
5. For east-positive longitude and UTC+8:

   ```text
   gamma = 2π / daysInYear * (dayOfYear - 1 + (hour - 12) / 24)
   equationOfTimeMinutes = 229.18 * (
       0.000075
       + 0.001868*cos(gamma)
       - 0.032077*sin(gamma)
       - 0.014615*cos(2*gamma)
       - 0.040849*sin(2*gamma)
   )
   longitudeMinutes = 4 * longitude - 60 * 8
   totalOffsetSeconds = round((equationOfTimeMinutes + longitudeMinutes) * 60)
   effectiveTime = originalCivilTime + totalOffsetSeconds
   ```

6. Preserve seconds during correction. Round only the final offset to seconds before passing the effective date/time to `Solar.fromYmdHms`.
7. Existing saved reports and records remain immutable snapshots and are not recalculated.

### Task 1: Lock the backend calculation contract with failing unit tests

**Files:**
- Create: `apps/server/src/test/java/com/bazi/app/service/BirthTimeResolverTest.java`
- Create: `apps/server/src/test/java/com/bazi/app/service/BirthPlaceRegistryTest.java`

**Step 1: Write the failing location tests**

Cover exact city resolution (`广东省 深圳市 -> 114.05`), municipality resolution (`上海市 黄浦区 -> 121.47`), unsupported text, and province-only input. Assert that unsupported/province-only input does not fall back silently.

**Step 2: Write the failing time tests**

Cover:

- disabled correction returns the exact input and no metadata;
- Shenzhen `1995-10-08T13:05:00` crosses from 未时 to 午时;
- a non-boundary example keeps the same 时辰;
- correction can cross a civil date boundary;
- leap-year equation-of-time calculation uses 366 days;
- result metadata contains original time, adjusted time, longitude, longitude offset, equation-of-time offset, and `boundaryChanged`.

**Step 3: Run tests to verify failure**

Run:

```bash
cd apps/server
./mvnw -Dtest=BirthPlaceRegistryTest,BirthTimeResolverTest test
```

Expected: compilation failure because the resolver classes do not exist.

**Step 4: Commit the red tests**

```bash
git add apps/server/src/test/java/com/bazi/app/service/BirthPlaceRegistryTest.java apps/server/src/test/java/com/bazi/app/service/BirthTimeResolverTest.java
git commit -m "test: define true solar time backend contract"
```

### Task 2: Add the server-side city longitude registry

**Files:**
- Create: `apps/server/src/main/resources/geo/city-geo.json`
- Create: `apps/server/src/main/java/com/bazi/app/service/BirthPlaceRegistry.java`
- Modify: `apps/web/src/data/cityGeo.json`
- Test: `apps/server/src/test/java/com/bazi/app/service/BirthPlaceRegistryTest.java`

**Step 1: Promote the existing 356-city coordinate data to the backend resource**

Copy the existing coordinates without changing values. The first batch keeps the request format `省 市`, but the registry resolves the city portion exactly and rejects missing cities. Do not retain the current provincial-capital fallback.

**Step 2: Implement deterministic lookup**

`BirthPlaceRegistry.longitudeOf(String birthPlace)` must resolve ordinary provinces by the selected city and explicitly resolve the four direct-administered municipalities by their municipality coordinate:

```java
public double longitudeOf(String birthPlace) {
  String[] parts = normalizeWhitespace(birthPlace).split(" ");
  if (parts.length != 2) throw unresolved();
  Double municipality = municipalityLongitude(parts[0]);
  if (municipality != null) return municipality;
  String city = stripAdministrativeSuffix(parts[1]);
  Double longitude = longitudeByCity.get(city);
  if (longitude == null) throw unresolved();
  return longitude;
}
```

Throw `BusinessException("BIRTH_PLACE_UNRESOLVED", "无法识别出生城市，请重新选择省市")` for unresolved input.

**Step 3: Add a data-parity check**

Add a test that loads both the backend and frontend JSON files and asserts identical city keys and longitude/latitude values. This prevents the two runtimes from drifting while mock mode still exists.

**Step 4: Run the focused tests**

Run:

```bash
cd apps/server
./mvnw -Dtest=BirthPlaceRegistryTest test
```

Expected: PASS.

**Step 5: Commit**

```bash
git add apps/server/src/main/resources/geo/city-geo.json apps/server/src/main/java/com/bazi/app/service/BirthPlaceRegistry.java apps/server/src/test/java/com/bazi/app/service/BirthPlaceRegistryTest.java
git commit -m "feat: resolve birth city longitude on server"
```

### Task 3: Implement the authoritative true-solar-time resolver

**Files:**
- Create: `apps/server/src/main/java/com/bazi/app/service/BirthTimeResolver.java`
- Create: `apps/server/src/main/java/com/bazi/app/service/ResolvedBirthTime.java`
- Modify: `apps/server/src/main/java/com/bazi/app/dto/TrueSolarDto.java`
- Test: `apps/server/src/test/java/com/bazi/app/service/BirthTimeResolverTest.java`

**Step 1: Implement strict input parsing**

Parse `solarDateTime` with `DateTimeFormatter.ISO_LOCAL_DATE_TIME`. Map invalid values to `BusinessException("BIRTH_TIME_INVALID", "出生时间格式不正确")` instead of allowing an internal 500 response.

**Step 2: Implement the NOAA equation-of-time calculation**

Use the formula in this plan and `Year.isLeap(year)` for the denominator. Keep the sign convention east-positive; for UTC+8 the longitude term is `4 * longitude - 480` minutes.

**Step 3: Return an internal resolved value**

```java
public record ResolvedBirthTime(
    LocalDateTime original,
    LocalDateTime effective,
    TrueSolarDto metadata) {}
```

`metadata` is `null` when the feature is disabled. Otherwise populate the existing response contract, including 时辰 names and whether the boundary changed.

**Step 4: Run the focused tests**

Run:

```bash
cd apps/server
./mvnw -Dtest=BirthTimeResolverTest test
```

Expected: PASS.

**Step 5: Commit**

```bash
git add apps/server/src/main/java/com/bazi/app/service/BirthTimeResolver.java apps/server/src/main/java/com/bazi/app/service/ResolvedBirthTime.java apps/server/src/main/java/com/bazi/app/dto/TrueSolarDto.java apps/server/src/test/java/com/bazi/app/service/BirthTimeResolverTest.java
git commit -m "feat: calculate true solar birth time on server"
```

### Task 4: Make every backend chart consumer use the resolved time

**Files:**
- Modify: `apps/server/src/main/java/com/bazi/app/service/BaziService.java:35-47`
- Modify: `apps/server/src/main/java/com/bazi/app/service/BaziService.java:181-204`
- Test: `apps/server/src/test/java/com/bazi/app/BaziServiceTest.java`

**Step 1: Add failing service tests**

Assert that:

- true-solar disabled fixtures remain byte-for-byte compatible for the existing asserted fields;
- enabled Shenzhen boundary input changes the time pillar;
- `solarText` is the effective time used for calculation;
- `trueSolar.original` retains the entered time and `trueSolar.adjusted` equals `solarText` at minute precision;
- the original `PaipanRequest` object is not mutated.

**Step 2: Resolve once at the top of `paipan`**

Replace manual string splitting with:

```java
ResolvedBirthTime resolved = birthTimeResolver.resolve(req);
LocalDateTime effective = resolved.effective();
Solar solar = Solar.fromYmdHms(
    effective.getYear(), effective.getMonthValue(), effective.getDayOfMonth(),
    effective.getHour(), effective.getMinute(), effective.getSecond());
```

Return `resolved.metadata()` in `PaipanResultDto.trueSolar`.

**Step 3: Preserve the no-argument test construction path**

Many current unit tests call `new BaziService()`. Retain a no-argument constructor backed by the default registry/resolver and add a package-visible injection constructor for focused tests; do not mechanically rewrite unrelated report tests.

**Step 4: Run backend tests**

Run:

```bash
cd apps/server
./mvnw test
```

Expected: all existing fixtures with `trueSolarTime=false` remain green; new true-solar tests pass.

**Step 5: Commit**

```bash
git add apps/server/src/main/java/com/bazi/app/service/BaziService.java apps/server/src/test/java/com/bazi/app/BaziServiceTest.java
git commit -m "feat: use resolved birth time for all server charts"
```

### Task 5: Prove records, reports, and checkout share one chart result

**Files:**
- Modify: `apps/server/src/test/java/com/bazi/app/BaziApiIntegrationTest.java`
- Modify: `apps/server/src/test/java/com/bazi/app/SavedReportIntegrationTest.java`
- Modify: `apps/server/src/test/java/com/bazi/app/ReportCheckoutIntegrationTest.java`
- Modify: `apps/server/src/test/java/com/bazi/app/ReportCheckoutFailureIntegrationTest.java`

**Step 1: Add an HTTP record integration case**

POST the original Shenzhen boundary input with `trueSolarTime=true`. Assert the returned time pillar and true-solar metadata are server-generated.

**Step 2: Add a report integration case**

Create a report from exactly the same raw request. Assert the stored `requestJson` still contains the original civil time and enabled flag, while the report evidence derives from the same corrected chart as `/records`.

**Step 3: Add a checkout failure case**

Submit `trueSolarTime=true` with an unresolved location. Assert:

- HTTP 400 with `BIRTH_PLACE_UNRESOLVED`;
- no locked report is inserted;
- no order is inserted;
- no member quota or payment is consumed.

**Step 4: Run integration tests**

Run:

```bash
cd apps/server
./mvnw -Dtest=BaziApiIntegrationTest,SavedReportIntegrationTest,ReportCheckoutIntegrationTest,ReportCheckoutFailureIntegrationTest test
```

Expected: PASS.

**Step 5: Commit**

```bash
git add apps/server/src/test/java/com/bazi/app/BaziApiIntegrationTest.java apps/server/src/test/java/com/bazi/app/SavedReportIntegrationTest.java apps/server/src/test/java/com/bazi/app/ReportCheckoutIntegrationTest.java apps/server/src/test/java/com/bazi/app/ReportCheckoutFailureIntegrationTest.java
git commit -m "test: keep chart and paid report birth time consistent"
```

### Task 6: Remove HTTP-side pre-correction and prevent double correction

**Files:**
- Modify: `apps/web/src/services/httpBaziApi.ts`
- Modify: `apps/web/src/lib/enrichResult.ts`
- Modify: `apps/web/src/lib/enrichResult.test.ts`
- Modify: `apps/web/src/services/reportApi.test.ts`
- Modify: `apps/web/src/services/payApi.test.ts`

**Step 1: Change the failing HTTP test expectation**

Assert that `HttpBaziApi.paipan` posts the original request unchanged, including `trueSolarTime=true` and the original `solarDateTime`.

**Step 2: Remove request adjustment from HTTP mode**

Change the HTTP implementation to:

```ts
const res = await authFetch('/api/v1/records', {
  method: 'POST',
  body: JSON.stringify(req),
})
return enrichResult((await res.json()) as PaipanResult)
```

Do not attach locally calculated `trueSolar`; accept the server metadata.

**Step 3: Retain local calculation only for mock/offline mode**

`baziMapper.paipan` may continue using `trueSolarTime.ts`, but `adjustRequestForTrueSolar` must no longer be part of any HTTP/report/payment path. Remove it if no production caller remains.

**Step 4: Assert all HTTP payloads use the same raw request**

Update record/report/checkout tests to assert the same request shape reaches the backend.

**Step 5: Run frontend tests**

Run:

```bash
cd apps/web
npm test -- src/services src/lib/enrichResult.test.ts
```

Expected: PASS.

**Step 6: Commit**

```bash
git add apps/web/src/services/httpBaziApi.ts apps/web/src/lib/enrichResult.ts apps/web/src/lib/enrichResult.test.ts apps/web/src/services/reportApi.test.ts apps/web/src/services/payApi.test.ts
git commit -m "fix: let server own true solar correction"
```

### Task 7: Tighten the input UI and make correction visible

**Files:**
- Modify: `apps/web/src/pages/InputPage.tsx`
- Modify: `apps/web/src/components/RegionSelect.tsx`
- Modify: `apps/web/src/pages/InputPage.test.tsx` (create if absent)
- Modify: `apps/web/src/pages/ChartPage.tsx`
- Modify: `apps/web/src/pages/ChartPage.test.tsx`

**Step 1: Require both province and city when correction is enabled**

Do not allow a province-only string to pass validation. Display: `开启真太阳时需要选择具体出生城市`.

**Step 2: Show the effective calculation clearly**

On the chart page display original time, corrected time, longitude, total correction minutes, and a prominent warning when the correction changes the date or 时辰. Do not imply a change when `boundaryChanged=false`.

**Step 3: Add the support boundary note**

Near the switch state: `当前按中国 UTC+8 城市经度校正；海外地点和历史特殊时制暂不自动处理。`

**Step 4: Run focused UI tests**

Run:

```bash
cd apps/web
npm test -- src/pages/InputPage.test.tsx src/pages/ChartPage.test.tsx
```

Expected: PASS.

**Step 5: Commit**

```bash
git add apps/web/src/pages/InputPage.tsx apps/web/src/components/RegionSelect.tsx apps/web/src/pages/InputPage.test.tsx apps/web/src/pages/ChartPage.tsx apps/web/src/pages/ChartPage.test.tsx
git commit -m "feat: clarify true solar input and result"
```

### Task 8: Update the contract, documentation, and full regression gate

**Files:**
- Modify: `contracts/openapi.yaml:912-930`
- Modify: `contracts/openapi.yaml:1157-1178`
- Modify: `docs/design/backend-design.md`
- Modify: `docs/mockups/bazi-app-mockups.md`
- Create: `docs/verification/true-solar-time-acceptance.md`

**Step 1: Document request semantics**

State that `solarDateTime` is always the original civil time and is corrected exactly once on the backend. Document `BIRTH_PLACE_UNRESOLVED` and the UTC+8/China-city support boundary.

**Step 2: Remove obsolete architecture statements**

Delete or replace documentation saying true solar time is implemented only by the frontend.

**Step 3: Record acceptance vectors**

Include at least:

- correction off;
- correction on without 时辰 change;
- Shenzhen boundary change;
- correction crossing a calendar date;
- unresolved place failure without charge;
- `/records`, `/reports`, and `/reports/checkout` consistency.

**Step 4: Run the full verification gate**

Run:

```bash
cd apps/server
./mvnw test
```

Expected: PASS.

Run:

```bash
cd apps/web
npm test
npm run lint
npm run build
```

Expected: all PASS.

**Step 5: Manually verify HTTP mode**

Start the server and the frontend with `VITE_API_MODE=http`. Generate a chart and a report from a boundary case, then verify both show evidence derived from the corrected time pillar.

**Step 6: Commit**

```bash
git add contracts/openapi.yaml docs/design/backend-design.md docs/mockups/bazi-app-mockups.md docs/verification/true-solar-time-acceptance.md
git commit -m "docs: define true solar time source of truth"
```

## Acceptance gate

The feature is complete only when all conditions hold:

- No HTTP frontend path changes `solarDateTime` before submission.
- The backend applies correction exactly once for records, reports, previews, and checkout.
- Stored request JSON preserves the user's original time and correction choice.
- Returned `trueSolar` metadata exposes the actual time used for calculation.
- Unknown/province-only places fail before report insertion, quota consumption, or order creation.
- Existing `trueSolarTime=false` fixtures remain unchanged.
- Boundary cases prove that chart and paid report use the same time pillar.
- Mock and backend calculation vectors differ by at most one second; any larger difference fails CI.
