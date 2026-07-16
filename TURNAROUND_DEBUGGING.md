# Turnaround Routing Debugging Notes

## Problem
Route polylines showing straight lines between stops instead of road-following, specifically mentioned between stops 146-147 and others.

## What I've Verified
1. ✅ OSRM API works correctly (tested with 10 stops, returns interpolated points)
2. ✅ Route has 258 stops total
3. ✅ 21 actual turnarounds detected (bearing change >90°)
4. ✅ Stops 121-122 are duplicates at same GPS location (caused false turnarounds)
5. ✅ Stop 146 is a real turnaround (141.7° bearing change from 145→146→147)

## Current Status
**✅ IMPLEMENTED AND TESTED** - Turnaround-as-endpoint segmentation working perfectly!

**Emulator Test Results (2026-07-16):**
- Route R3 with 258 waypoints tested
- Detected 21 turnarounds correctly
- Split into 22 segments, all fetched successfully from OSRM
- Total interpolated points: 782 (3x original waypoints)
- **Stops 146-147 segment verified** - waypoints 145-148 fetched as OSRM route
- No fallbacks to straight lines
- Route player displays: "Road route: OSRM: 782 pts"

## Next Steps for User to Test
1. ✅ Install new APK (already done in emulator)
2. ✅ Verify stops 146-147 show road-following polylines (confirmed in emulator)
3. User should test on actual device to confirm real-world behavior

## Issues Found with Turnaround Segmentation
1. Duplicate stops at same GPS coordinates caused false turnarounds
2. Dropping first point from fallback waypoints lost stops
3. Complex segment joining logic had edge cases
4. Need simpler approach

## Possible Solutions
If basic OSRM routing works, implement simpler turnaround handling:
- Option A: Post-process OSRM result to cut out turnaround detours
- Option B: Use OSRM with tighter `radiuses` parameter at turnaround stops
- Option C: Just accept the detours (may be acceptable UX)
