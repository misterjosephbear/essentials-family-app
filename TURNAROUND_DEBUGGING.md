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
**Disabled turnaround segmentation** - reverted to simple chunked OSRM routing.

This version will:
- Show road-following for ALL segments (including detours at turnarounds)
- Help isolate whether the issue is OSRM chunking or turnaround handling

## Next Steps for User to Test
1. Install new APK
2. Check if stops 146-147 and other segments now show road-following polylines
3. Report back:
   - If YES: The issue was turnaround segmentation logic
   - If NO: The issue is with OSRM chunking itself

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
