# Variant Labels

Files in this folder are loaded from client resources at:

```text
assets/better_pokedex_scanner/variant_labels/*.json
```

Multiple files can exist. They are merged in resource-pack order when resources reload.

This system is used for Cobblemon form/aspect display labels.

Labels can now be declared in four ways:

1. Plain literal text
2. Plain translation key text
3. Styled `parts`
4. Styled translated templates with `translationKey` and `args`

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
      "aspectDisplays": {
        "special-marking": {
          "parts": [
            { "text": "Special", "color": "F5D76E" },
            { "text": " Marking" }
          ]
        }
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

- `baseFormDisplay`
  - Structured styled display for the base form.
  - If present, it takes priority over both `baseFormTranslationKey` and `baseFormLabel`.

- `formLabels`
  - Map of raw form names to literal display labels.
  - Keys are matched case-insensitively.
  - `_` and `-` are treated the same when matching.

- `formTranslationKeys`
  - Map of raw form names to translation keys.
  - If a matching key is found here, the translated text is shown instead of a literal label.

- `formDisplays`
  - Map of raw form names to structured styled displays.
  - If a matching key is found here, it takes priority over `formTranslationKeys` and `formLabels`.

- `aspectLabels`
  - Map of raw aspect names to literal display labels.

- `aspectTranslationKeys`
  - Map of raw aspect names to translation keys.

- `aspectDisplays`
  - Map of raw aspect names to structured styled displays.
  - If a matching key is found here, it takes priority over `aspectTranslationKeys` and `aspectLabels`.

- `missingAllAspects`
  - Optional list of aspects that should all be absent before this group emits a fallback label.

- `missingAspectLabel`
  - Literal label used when every aspect in `missingAllAspects` is missing.

- `missingAspectTranslationKey`
  - Translation key version of `missingAspectLabel`.

- `missingAspectDisplay`
  - Structured styled display used when every aspect in `missingAllAspects` is missing.
  - If present, it takes priority over `missingAspectTranslationKey` and `missingAspectLabel`.

## Styled Display Shape

A styled display can be declared in one of two ways.

### 1. `parts`

Use this for English-first labels or simple pack-local formatting.

```json
{
  "parts": [
    { "text": "Red", "color": "FF0000" },
    { "text": " Flower" }
  ]
}
```

Each part supports:

- `text`
  - Literal text for that segment.
- `translationKey`
  - Translation key for that segment instead of literal text.
- `color`
  - Optional six-digit hex color such as `"FF0000"` or `"#FF0000"`.

### 2. `translationKey` + `args`

Use this when you want the full phrase structure to be controlled by language files.

```json
{
  "translationKey": "betterpokedexscanner.aspectLabel.flower_colour",
  "args": [
    { "translationKey": "betterpokedexscanner.colour.red", "color": "FF0000" }
  ]
}
```

Then in your lang file:

```json
{
  "betterpokedexscanner.aspectLabel.flower_colour": "%s Flower",
  "betterpokedexscanner.colour.red": "Red"
}
```

This is the better option if a language may need a different word order than English.

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

You can also localize styled templates:

```json
{
  "groups": [
    {
      "headerTranslationKey": "betterpokedexscanner.header.flower_color",
      "species": ["cobblemon:flabebe"],
      "aspectDisplays": {
        "flower-red": {
          "translationKey": "betterpokedexscanner.aspectLabel.flower_colour",
          "args": [
            { "translationKey": "betterpokedexscanner.colour.red", "color": "FF0000" }
          ]
        }
      }
    }
  ]
}
```

## Notes

- Literal labels are useful for quick pack-local setup.
- Translation keys are better if you support multiple languages.
- Styled displays are best when you want partial coloring or mixed formatting.
- For any single output, resolution priority is:
  1. styled display
  2. translation key
  3. literal label
