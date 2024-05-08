# COMP 4321

## Instructions

Build and run crawler with

```bash
$ scripts/complie_and_run.sh
```

Prepare the search engine

```bash
$ scripts/search.sh
```

Start the web server

```bash
$ scripts/start_server.sh
```

## Custom Class

### `PageInfo`

Contains information of a page.
| Attributes | Type | Definition |
|------------|---------------------|------------------------|
| `pageTitle` | `String` | Page's title|
| `date` | `Date` | Last modification date |
| `size` | `int` | Page's size|
| `childUrls` | `ArrayList<String>` | List of child's urls |
| `parentUrls` | `ArrayList<String>` | List of parent's urls |

## Databases (`HTree`)

All the data are stored in `database.db`.

### `urlPageIDMapForward`

Mapping from URL to an unique page ID.
||Attribute|Type|
|-|-|-|
|Key|`url`|`String`|
|Value|`pageID`|`int`|

### `urlPageIDMapBackward`

Mapping from page ID to an Url.
||Attribute|Type|
|-|-|-|
|Key|`pageID`|`int`|
|Value|`url`|`String`|

### `wordIDMapForward`

Mapping from word to word's ID.
||Attribute|Type|
|-|-|-|
|Key|`word`|`String`|
|Value|`wordID`|`int`|

### `wordIDMapBackward`

Mapping from word's ID to word.
||Attribute|Type|
|-|-|-|
|Key|`wordID`|`int`|
|Value|`word`|`String`|

### `wordBodyFreqMap${pageID}`

Mapping from word's ID to frequency in url body with `${pageID}`
||Attribute|Type|
|-|-|-|
|Key|`wordID`|`int`|
|Value|`frequency`|`int`|

### `wordTitleFreqMap${pageID}`

Mapping from word's ID to frequency in url title with `${pageID}`
||Attribute|Type|
|-|-|-|
|Key|`wordID`|`int`|
|Value|`frequency`|`int`|

### `parentIDPageInfoMap`

Mapping from parent pages to the page information.
||Attribute|Type|
|-|-|-|
|Key|`parentUrl`|`String`|
|Value|`page_info`|`PageInfo`|

### `childIDPageInfoMap`

Mapping from child pages to the page information.
||Attribute|Type|
|-|-|-|
|Key|`childUrl`|`String`|
|Value|`page_info`|`PageInfo`|

---

## Indexing

We index web pages base on the following logic:

1. We split lines into tokens using space and other speical charaters.
2. We set tokens to lowercase.
3. We remove all the words listed in `stopword.txt`.
4. We use Porter's algorithm to strip token's affiexes.
