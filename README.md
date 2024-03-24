# COMP 4321

## Custom Class

## `PageInfo`

Contains information of a page.
| Attributes | Type | Definition |
|------------|---------------------|------------------------|
| `date` | `Date` | Last modification date |
| `wordsIDCount` | `HTree` | Mapping from word's ID to word frequency.|
| `childUrls` | `ArrayList<String>` | List of child's urls |
| `parentUrls` | `ArrayList<String>` | List of parent's urls |

## Databases (`HTree`)

## `urlPageIDMapForward`

Mapping from URL to an unique page ID.
||Attribute|Type|
|-|-|-|
|Key|`url`|`String`|
|Value|`pageID`|`Int`|

## `urlPageIDMapBackward`

Mapping from page ID to an Url.
||Attribute|Type|
|-|-|-|
|Key|`pageID`|`Int`|
|Value|`url`|`String`|

## `wordIDMapForward`

Mapping from word to word's ID.
||Attribute|Type|
|-|-|-|
|Key|`word`|`String`|
|Value|`wordID`|`Int`|

## `wordIDMapBackward`

Mapping from word's ID to word.
||Attribute|Type|
|-|-|-|
|Key|`wordID`|`Int`|
|Value|`word`|`String`|

## `wordFreqMap${pageID}`

Mapping from word's ID to frequency in url with `${pageID}`
||Attribute|Type|
|-|-|-|
|Key|`wordID`|`Int`|
|Value|`frequency`|`Int`|

## `parentIDPageInfoMap`

Mapping from parent pages to the page information.
||Attribute|Type|
|-|-|-|
|Key|`parentUrl`|`String`|
|Value|`page_info`|`PageInfo`|

## `childIDPageInfoMap`

Mapping from child pages to the page information.
||Attribute|Type|
|-|-|-|
|Key|`childUrl`|`String`|
|Value|`page_info`|`PageInfo`|

---
