# Entity Accessor Labels

Files in this folder are loaded from client resources at:

```text
assets/better_pokedex_scanner/entity_accessor_labels/*.json
```

Multiple files can exist. They are merged in resource-pack order when resources reload.

This system is intended for non-Pokemon entities, especially custom mobs that expose useful data through a field or accessor method that the scanner does not know about natively.

Resolved accessor displays are added as a normal non-Pokemon variant-style page.

This path is explicit-only. If an entity is not declared here, the scanner will not try to discover arbitrary custom accessor fields automatically.

Accessor value displays can now be declared in four ways:

1. Plain literal text
2. Plain translation key text
3. Styled `parts`
4. Styled translated templates with `translationKey` and `args`

## File Shape

```json
{
  "groups": [
    {
      "entities": ["examplemod:example_mob"],
      "accessor": "variant",
      "accessorHeaderLabel": "Mutation",
      "valueDisplays": {
        "red": {
          "parts": [
            { "text": "Red", "color": "FF0000" }
          ]
        }
      },
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

- `valueDisplays`
  - Map of raw accessor values to structured styled displays.
  - If present for a matching value, it takes priority over `valueTranslationKeys` and `valueLabels`.

## Styled Display Shape

A styled display can be declared in one of two ways.

### 1. `parts`

Use this for straightforward English-first labels or when only part of the value should be colored.

```json
{
  "parts": [
    { "text": "Blue", "color": "4A90E2" },
    { "text": " Wool" }
  ]
}
```

Each part supports:

- `text`
  - Literal text for that segment.
- `translationKey`
  - Translation key for that segment instead of literal text.
- `color`
  - Optional six-digit hex color such as `"4A90E2"` or `"#4A90E2"`.

### 2. `translationKey` + `args`

Use this when the full phrase should be controlled by the language file.

```json
{
  "translationKey": "betterpokedexscanner.entityAccessor.colour_variant",
  "args": [
    { "translationKey": "betterpokedexscanner.colour.blue", "color": "4A90E2" }
  ]
}
```

Then in your lang file:

```json
{
  "betterpokedexscanner.entityAccessor.colour_variant": "%s Variant",
  "betterpokedexscanner.colour.blue": "Blue"
}
```

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

- If the raw accessor value matches `valueDisplays`, the structured styled display is used.
- Otherwise, if it matches `valueTranslationKeys`, the translated text is used.
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

Styled template example:

```json
{
  "groups": [
    {
      "entities": ["examplemod:example_mob"],
      "accessor": "variant",
      "valueDisplays": {
        "blue": {
          "translationKey": "betterpokedexscanner.entityAccessor.colour_variant",
          "args": [
            { "translationKey": "betterpokedexscanner.colour.blue", "color": "4A90E2" }
          ]
        }
      }
    }
  ]
}
```

## Notes

- This system is best used for custom mobs with stable accessors.
- It is explicit-only. New custom accessors must be declared here to appear.
- Built-in hardcoded entity handling still exists for the vanilla cases the scanner already knows how to present.
- Accessor traversal is reflective, so keep paths simple and predictable.
- For any single value, resolution priority is:
  1. styled display
  2. translation key
  3. literal label
