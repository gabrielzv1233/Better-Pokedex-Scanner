# Variant Labels

Files in this folder are loaded from client resources at:

```text
assets/better_pokedex_scanner/variant_labels/*.json
```

Multiple files can exist. They are merged in resource-pack order when resources reload.

This system is used for Cobblemon form/aspect display labels.

## File Shape

```json
{
  "groups": [
    {
      "headerLabel": "Regional Variation",
      "species": ["cobblemon:vulpix", "cobblemon:ninetales"],
      "formLabels": {
        "alolan": "Alolan"
      },
      "aspectTranslationKeys": {
        "special-marking": "betterpokedexscanner.aspectLabel.special_marking"
      }
    }
  ]
}
```

## Group Fields

- `headerLabel`
  - Literal header text shown above matching labels.
  - Example: `"Regional Variation"`

- `headerTranslationKey`
  - Translation key used for the header instead of a literal label.
  - If present, it takes priority over `headerLabel`.

- `species`
  - Optional list of species ids this group applies to.
  - If omitted or empty, the group can match any species.
  - Example: `["cobblemon:flabebe"]`

- `baseFormLabel`
  - Literal label to use when the Pokemon is on its base form and you still want a display.

- `baseFormTranslationKey`
  - Translation key version of `baseFormLabel`.
  - If present, it takes priority over `baseFormLabel`.

- `formLabels`
  - Map of raw form names to literal display labels.
  - Keys are matched case-insensitively.
  - `_` and `-` are treated the same when matching.

- `formTranslationKeys`
  - Map of raw form names to translation keys.
  - If a matching key is found here, the translated text is shown instead of a literal label.

- `aspectLabels`
  - Map of raw aspect names to literal display labels.

- `aspectTranslationKeys`
  - Map of raw aspect names to translation keys.

- `missingAllAspects`
  - Optional list of aspects that should all be absent before this group emits a fallback label.

- `missingAspectLabel`
  - Literal label used when every aspect in `missingAllAspects` is missing.

- `missingAspectTranslationKey`
  - Translation key version of `missingAspectLabel`.

## Matching Rules

- Species ids are compared case-insensitively.
- Form/aspect keys are compared case-insensitively.
- `light_gray` and `light-gray` are treated as the same key.
- The first matching group wins.

## Translation Keys

You can localize both headers and values.

Example:

```json
{
  "groups": [
    {
      "headerTranslationKey": "betterpokedexscanner.header.flower_color",
      "species": ["cobblemon:flabebe"],
      "aspectTranslationKeys": {
        "flower-red": "betterpokedexscanner.aspectLabel.red_flower"
      }
    }
  ]
}
```

Then add the keys in your language files:

```json
{
  "betterpokedexscanner.header.flower_color": "Flower Color",
  "betterpokedexscanner.aspectLabel.red_flower": "Red Flower"
}
```

## Notes

- Literal labels are useful for quick pack-local setup.
- Translation keys are better if you support multiple languages.
- If both a literal field and a translation-key field are set for the same output, the translation key wins.
