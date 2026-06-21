# UI Customization via dynamicForms

`dynamicForms` provides hints to the WebUI (vibeui) on how to render elements.

## Struct Properties

| Property | Type | Description |
| :--- | :--- | :--- |
| `label` | String | Title of the panel/group. |
| `width` | String | Total width of the form (e.g., '50%', '800px'). |
| `container` | String | CSS Grid class for the group (e.g., `ui-g-12`). |
| `hidden` | Boolean | If `true`, hides the entire group. |
| `required` | Boolean | If `true`, makes the group mandatory. |

## Field Properties

| Property | Type | Description |
| :--- | :--- | :--- |
| `label` | String | Custom label for the field. |
| `type` | String | Widget type (e.g., `SELECT`, `TEXTAREA`, `PASSWORD`, `RADIO`). |
| `inputType` | String | HTML input type (e.g., `text`, `password`, `file`). |
| `hidden` | Boolean | Hides the field. |
| `disabled` | Boolean | Disables input. |
| `required` | Boolean | Makes the field mandatory. |
| `multiple` | Boolean | Allows multiple values (e.g., in a select). |
| `placeholder` | String | Placeholder text. |
| `mask` | String | Input mask (PrimeNG compatible). |
| `min` / `max` | Integer | Minimum and maximum values. |
| `value` | String | Default value for the UI. |
| `container` | String | Grid class for field + label (Default: `ui-g-12`). |
| `labelGrid` | String | Grid class for label (Default: `ui-g-4`). |
| `control` | String | Grid class for input control (Default: `ui-g-8`). |
| `updateFields` | List<String> | Fields to refresh when this field changes. |
| `updateScriptRef` | String/Script| Script to execute on update. |
| `updateQuerytRef` | String/Query | Query to execute on update. |
| `precision` / `scale` | String | For numeric formatting (e.g., '5', '5-'). |
| `showSeconds` | Boolean | For time/date fields. |
| `htmlAccept` | String | For file uploads (e.g., '.pdf,.doc'). |
| `autoComplete` | String | Browser autocomplete hint. |

## List of Values (listOfValues)
Used within `field` to provide options for selection widgets.

| Property | Type | Description |
| :--- | :--- | :--- |
| `scriptRef` | String/Script| Script to fetch values. |
| `queryRef` | String/Query | Query to fetch values. |
| `propertyNames` | String | Comma-separated ItemProperty names. |
| `inputName` | String | Name of the input variable for the script/query. |
| `values` | List<String> | Static list of options. |

## Examples

### Layout and Update
```groovy
field(name: 'Country', type: 'string') {
    dynamicForms(type: 'SELECT', updateFields: ['City'])
    listOfValues(queryRef: 'GetCountries:0')
}
field(name: 'City', type: 'string') {
    dynamicForms(type: 'SELECT')
    listOfValues(queryRef: 'GetCities:0', inputName: 'Country')
}
```
