# JDBM Database Schema for Web Crawler and Indexer

This document outlines the design of the JDBM database schema used for the web crawler and indexer.

## Database Components Overview

The schema comprises several key databases, each serving a specific purpose in the indexing process:

1. **URL to Page ID Map (Forward and Backward)**
2. **Word to Word ID Map (Forward and Backward)**
3. **Word Frequency Map per Page**
4. **Parent and Child Page Information Maps**

## Detailed Schema Description

### 1. URL to Page ID Map (Forward and Backward)

- **Purpose:** Facilitates quick lookups by mapping URLs to unique page IDs and vice versa.
  
#### Implementation Details
- **URL to Page ID Map Forward:** 
  - **Key:** URL (String)
  - **Value:** Page ID (Integer)
- **Page ID to URL Map Backward:** 
  - **Key:** Page ID (Integer)
  - **Value:** URL (String)

#### Rationale
The bidirectional mapping between URLs and page IDs enables quick lookups in both directions. Using page IDs as unique identifiers for pages reduces storage space and lookup time compared to using URLs directly in all instances.

### 2. Word to Word ID Map (Forward and Backward)

- **Purpose:** Maps words to unique word IDs and vice versa, crucial for indexing and normalized word occurrence storage.
  
#### Implementation Details
- **Word to Word ID Map Forward:** 
  - **Key:** Word (String)
  - **Value:** Word ID (Integer)
- **Word ID to Word Map Backward:** 
  - **Key:** Word ID (Integer)
  - **Value:** Word (String)

#### Rationale
This design supports efficient text processing and indexing by using numerical identifiers for words, significantly reducing the storage requirements and speeding up word lookup operations. It is especially beneficial for indexing and search operations, where the same words can occur across many documents.

### 3. Word Frequency Maps (Body and Title)

- **Purpose:** Maps word IDs to their frequency in the page body and title, supporting detailed indexing and search operations.
  
#### Implementation Details
- **Body Frequency Map${pageID}:** 
  - **Key:** Word ID (Integer)
  - **Value:** Frequency (Integer)
- **Title Frequency Map${pageID}:**
  s- **Key:** Word ID (Integer)
  - **Value:** Frequency (Integer)

#### Rationale
Storing word frequencies in separate maps for each page body and title allows the system to quickly access and update the frequency of words on a per-page basis. This structure is crucial for building and updating the inverted index used in search operations, enabling efficient keyword searches and relevance scoring.

### 4. Parent and Child Page Information Maps

- **Purpose:** These mappings associate parent and child pages with their PageInfo objects. The parent map links a parent URL to its page information, while the child map links a child URL to its page information.

#### Implementation Details
- **Parent ID Page Info Map:**
  - **Key:** Parent URL (String)
  - **Value:** PageInfo (Custom Object including attributes for date, size, title, child URLs)
- **Child ID Page Info Map:**
  - **Key:** Child URL (String)
  - **Value:** PageInfo (Custom Object including attributes for date, size, title, parent URLs)

#### Rationale
Tracking the relationships between pages helps in understanding the web structure and supports features like link analysis and content prioritization based on page hierarchies.


