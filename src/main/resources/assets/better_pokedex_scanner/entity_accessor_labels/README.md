# Entity Accessor Labels

Files in this folder are loaded from client resources at:

```text
assets/better_pokedex_scanner/entity_accessor_labels/*.json
```

Multiple files can exist. They are merged in resource-pack order when resources reload.

This system is intended for non-Pokemon entities, especially custom mobs that expose useful data through a field or accessor method that the scanner does not know about natively.

Resolved accessor displays are added as a normal non-Pokemon variant-style page.

This path is explicit-only. If an entity is not declared here, the scanner will not try to discover arbitrary custom accessor fields automatically.

## File Shape

```json
{
  "groups": [
    {
      "entities": ["examplemod:example_mob"],
      "accessor": "variant",
      "accessorHeaderLabel": "Mutation",
      "valueLabels": {
        "red": "Red",
        "blue": "Blue"
      }
    }
  ]
}
```

## Group Fields

- `entities`
  - Required list of entity type ids this accessor group applies to.
  - Example: `["minecraft:slime"]`

- `accessor`
  - Required accessor path to resolve from the entity.
  - Supports dot-separated traversal.
  - Each segment may resolve through:
    - a zero-argument method with the exact segment name
    - a zero-argument getter like `getVariant`
    - a zero-argument boolean getter like `isBaby`
    - a public field with the same name
  - Examples:
    - `"variant"`
    - `"data.variant"`
    - `"size"`
    - `"variant.base"`

- `accessorHeaderLabel`
  - Literal header text for this accessor display.
  - Example: `"Size Class"`

- `accessorHeaderTranslationKey`
  - Translation key version of `accessorHeaderLabel`.
  - If present, it takes priority over `accessorHeaderLabel`.

- `valueLabels`
  - Map of raw accessor values to literal display labels.
  - Keys are matched case-insensitively.
  - `_` and `-` are treated the same when matching.

- `valueTranslationKeys`
  - Map of raw accessor values to translation keys.
  - If present for a matching value, the translated text is shown instead of a literal label.

## Matching Rules

- Entity ids are compared case-insensitively.
- Value keys are compared case-insensitively.
- `light_gray` and `light-gray` are treated as the same key.
- The first matching group for an entity type is used.

## Header Behavior

- If you provide `accessorHeaderTranslationKey`, it is used for the header.
- Otherwise, if you provide `accessorHeaderLabel`, it is used for the header.
- If no custom header is provided at all, the scanner falls back to the normal `Variant` header.

## Value Behavior

- If the raw accessor value matches `valueTranslationKeys`, the translated text is used.
- Otherwise, if it matches `valueLabels`, the literal label is used.
- Otherwise, the scanner falls back to formatting the raw value automatically.

Examples of automatic formatting:

- `PURPLE` -> `Purple`
- `light_gray` -> `Light Gray`
- `blue_variant` -> `Blue Variant`

## Translation Keys

Example:

```json
{
  "groups": [
    {
      "entities": ["examplemod:example_mob"],
      "accessor": "variant",
      "accessorHeaderTranslationKey": "betterpokedexscanner.header.mutation",
      "valueTranslationKeys": {
        "blue": "betterpokedexscanner.entityAccessor.blue"
      }
    }
  ]
}
```

Then define those keys in your language files:

```json
{
  "betterpokedexscanner.header.mutation": "Mutation",
  "betterpokedexscanner.entityAccessor.blue": "Blue"
}
```

## Notes

- This system is best used for custom mobs with stable accessors.
- It is explicit-only. New custom accessors must be declared here to appear.
- Built-in hardcoded entity handling still exists for the vanilla cases the scanner already knows how to present.
- Accessor traversal is reflective, so keep paths simple and predictable.
