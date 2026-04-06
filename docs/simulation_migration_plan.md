# Simulation Migration Plan

Status as of 2026-04-05.

1. Phase 1: Extract the simulation runtime out of the station block entity.
Status: Completed

2. Phase 2: Make `FOOD_MATTER` the canonical food-state component and keep `INGREDIENT_STATE` as a derived compatibility cache.
Status: Completed

3. Phase 3: Build material profiles and recognition traits for the ingredient catalog.
Status: Completed

4. Phase 4: Replace more exact station execution with reusable simulation domains.
Status: Completed

5. Phase 5: Demote JSON recipes from hard gates to guided best-fit matching.
Status: Completed

6. Phase 6: Generalize recognizers and evaluation so simulated food is judged from physical state.
Status: Completed

7. Phase 7: Move storage, spoilage, and preservation onto the same canonical simulation model.
Status: Completed

8. Phase 8: Rebuild the station UI around simulation domains, richer feedback, and domain-specific controls.
Status: Pending
