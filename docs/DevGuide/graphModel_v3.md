<!-- textlint-disable terminology -->
# Graph data model v3

Inventory tool stores all collected data into Graph. Graph has following structure:

## Overview

![schema](https://uml.planttext.com/plantuml/png/ZLJBRiCW43oNhq0-sOTKFVPKfKsQAb8YrMr_W62SeDfWOQmqglBlvM5Z38xKYyNkx8lPmSjM8Y6xkWAWnQWYyE7-4O2IgjClW50GB14xzXQ4wPQLNDH8Kiuowzafl4D7qfiRWMfovk8x0tVf6vBaZ2uQkeAvSfyDmRIas2_ujT_SJb3z1MgbwB3inD1pXV29icDlxVF-i4OI7L0xH6rPGNvSeokuzlTCp1Tp9WcJRJ9qMbFTd8lBS34LbUaanHSj4POmAKX3L566AHdpTlGWaCvIeUFu2PSd8jf41wMlGd01LxmWOLI-VOVuX2Xh5GylwynMJPqx6onIaLgDQ_BJ3yvb1XUB9pKrhqsOxvliB00cPFfOi1aVUcwQoW5WQ6cn5QyRpinaaxWOtVEYKunQm97cJe4VJ6vXGKzg8MfodJ0ZLCDEco3aeMVIgMS6j-oDUfeluNHveu9MAWYOjfdc3G2FB-lkEGi0ZVYIYb5For5Dg-l8RIQ8l1wdKHrl6z7S-OnpP2T49io6eBUNgA8dhn1pw_R8c10GkZtEuchUW_JE8-QUvyIQUGJvFvECL9RgRwD-rNy0)

[Source](https://www.planttext.com/?text=ZLJBRiCW43oNhq0-sOTKFVPKfKsQAb8YrMr_W62SeDfWOQmqglBlvM5Z38xKYyNkx8lPmSjM8Y6xkWAWnQWYyE7-4O2IgjClW50GB14xzXQ4wPQLNDH8Kiuowzafl4D7qfiRWMfovk8x0tVf6vBaZ2uQkeAvSfyDmRIas2_ujT_SJb3z1MgbwB3inD1pXV29icDlxVF-i4OI7L0xH6rPGNvSeokuzlTCp1Tp9WcJRJ9qMbFTd8lBS34LbUaanHSj4POmAKX3L566AHdpTlGWaCvIeUFu2PSd8jf41wMlGd01LxmWOLI-VOVuX2Xh5GylwynMJPqx6onIaLgDQ_BJ3yvb1XUB9pKrhqsOxvliB00cPFfOi1aVUcwQoW5WQ6cn5QyRpinaaxWOtVEYKunQm97cJe4VJ6vXGKzg8MfodJ0ZLCDEco3aeMVIgMS6j-oDUfeluNHveu9MAWYOjfdc3G2FB-lkEGi0ZVYIYb5For5Dg-l8RIQ8l1wdKHrl6z7S-OnpP2T49io6eBUNgA8dhn1pw_R8c10GkZtEuchUW_JE8-QUvyIQUGJvFvECL9RgRwD-rNy0)

### Processing issues field

All vertex types may include an optional `processingIssues` array to capture problems detected while building or enriching the graph. Each array element records a single issue and follows the schema below. Unless a producer can provide the data, it should omit the field.

| Property  | Type     | Mandatory | Description |
|:----------|:---------|:----------|:------------|
| category  | String   | yes       | High-level issue type such as `parsing`, `processing`, `validation`, `io`, or `data-integrity`. |
| summary   | String   | yes       | Short human-readable description suitable for dashboards and reports. |
| source    | Object   | no        | Identifies the producer, e.g. `{ "task": "ParseErrorCodesVerticle", "stage": "error-code-enrichment" }`. |
| affected  | Object   | no        | Highlights a related sub-entity, e.g. `{ "type": "errorCode", "id": "ERR_123" }`. |
| timestamp | String   | no        | ISO-8601 timestamp indicating when the issue was captured. |
| details   | Object   | no        | Additional evidence, such as `{ "field": "errorCode", "rawValue": "" }`, exception metadata, or other context. |

## Vertex description
### Root
Anchor vertex. Can be used as a start point. Contains meta-info for the current graph.

Incoming Edges: not provided

Outgoing Edges:

1\. id: \<ID\>

![schema](https://uml.planttext.com/plantuml/png/SoWkIImgAStDuU8gJaxCILKmqBFWujAmIR8BoiyliT5FijDKqDMrKz1moSzDpCpJj5BGCDJ49kzOK92JcPnOafcFHv5dNbf-KIvXHKM9nIL5fSabfKMfkDK55wGcPwQXsidba9gN0lGD0000)

[Source](https://www.planttext.com/?text=SoWkIImgAStDuU8gJaxCILKmqBFWujAmIR8BoiyliT5FijDKqDMrKz1moSzDpCpJj5BGCDJ49kzOK92JcPnOafcFHv5dNbf-KIvXHKM9nIL5fSabfKMfkDK55wGcPwQXsidba9gN0lGD0000)

#### Data Model {#root-data-model}

| Property                           | Type     | Mandatory | Description                                                                                                             |
|:-----------------------------------|:---------|:----------|:------------------------------------------------------------------------------------------------------------------------|
| id                                 | String   | yes       | Value is "root"                                                                                                         |
| type                               | String   | yes       | Value is "root"                                                                                                         |
| name                               | String   | yes       | Value is "root"                                                                                                         |
| meta                               | Object   | no        | Contains meta-info describing this graph                                                                                |
| meta/type                          | String   | no        | Type of entity described by this graph. One of the following values: "component", "application", "namespace", "unknown" |
| meta/name                          | String   | no        | Name of entity described by this graph                                                                                  |
| meta/version                       | String   | no        | Version of entity described by this graph                                                                               |
| assembly                           | Object   | no        | Meta-info of graphs this graph was assembled from                                                                       |
| assembly/sourceGraphs              | Object[] | no        | Meta-info of source graphs successfully merged into this one                                                            |
| assembly/sourceGraphs[]/type       | String   | no        | See meta/type                                                                                                           |
| assembly/sourceGraphs[]/name       | String   | no        | See meta/name                                                                                                           |
| assembly/sourceGraphs[]/version    | String   | no        | See meta/version                                                                                                        |
| assembly/sourceGraphs[]/fileName   | String   | no        | Source filename                                                                                                        |
| assembly/sourceGraphs[]/assembly   | Object[] | no        | Has same structure as assembly/sourceGraphs, recursively                                                                |
| assembly/sourcesDropped            | Object[] | no        | Meta-info of source graphs that were considered invalid and dropped during merge(s)                                     |
| assembly/sourcesDropped[]/reason   | String   | no        | Reason why that graph was considered invalid                                                                            |
| assembly/sourcesDropped[]/type     | String   | no        | See meta/type                                                                                                           |
| assembly/sourcesDropped[]/name     | String   | no        | See meta/name                                                                                                           |
| assembly/sourcesDropped[]/version  | String   | no        | See meta/version                                                                                                        |
| assembly/sourcesDropped[]/fileName | String   | no        | Source filename                                                                                                        |

### Domain
Domain vertex represents Teams hierarchy.

Incoming Edges

1\. id: \<ID\>

Outgoing Edges:

2\. id: \<ID\>


![schema](https://uml.planttext.com/plantuml/png/SoWkIImgAStDuU8gJaxCILKmqBFWuj88oiylqLJGrRLJqB19idF9pqtCpBFHJxBJj5BGCDJaGXE4A7JEpot8pqlDAm4fCjBaud98pKi1cWm0)

[Source](https://www.planttext.com/?text=SoWkIImgAStDuU8gJaxCILKmqBFWuj88oiylqLJGrRLJqB19idF9pqtCpBFHJxBJj5BGCDJaGXE4A7JEpot8pqlDAm4fCjBaud98pKi1cWm0)

#### Data Model {#domain-data-model}

| Property          | Type   | Mandatory | Description                                              |
|:------------------|:-------|:----------|:---------------------------------------------------------|
| id                | String | yes       | Unique ID (started with "D_")                            |
| type              | String | yes       | Value is "domain"                                        |
| name              | String | yes       | Domain name                                              |
| abbreviation      | String | yes       | Domain abbreviation (same as ID but without prefix "D_") |
| technical-manager | String | yes       | Technical manager name                                   |
| department        | String | yes       | The name of the Department to which the Domain belongs   |
| env               | Map    | no        | Only for Infra Domain                                    |

### Application
Application vertex represents Application (Deployment Artifact) entity.

Incoming Edges

1\. id: \<ID\>

Outgoing Edges:

2\. id: \<ID\>

![schema](https://uml.planttext.com/plantuml/png/SoWkIImgAStDuU8gJaxCILKmqBFWuj88oiylqLJGrRLJqB19idCiACZ9J4uioSpFiz5FijEqKj0mrEJ29WFHufoVMv2VbvfN0b9ffCd5vP2QbmAq0m00)

[Source](https://www.planttext.com/?text=SoWkIImgAStDuU8gJaxCILKmqBFWuj88oiylqLJGrRLJqB19idCiACZ9J4uioSpFiz5FijEqKj0mrEJ29WFHufoVMv2VbvfN0b9ffCd5vP2QbmAq0m00)

#### Data Model {#application-data-model}

| Property | Type   | Mandatory | Description                                                |
|:---------|:-------|:----------|:-----------------------------------------------------------|
| id       | String | yes       | Unique ID (autogenerated as: application:<name>:<version>) |
| type     | String | yes       | Value is "application"                                     |
| name     | String | yes       | Application name                                           |
| version  | String | yes       | Application version                                        |

### Component
Component vertex represents Microservice, Internal Library, UI or another unit with source repository.

Incoming Edges

1\. id: \<ID\>

2\. id: \<ID\>, type: "mandatory" | "optional" | "startup"|"graphql", protocol: "http", viaZookeeper: "yes" | "no" | "not required", reference: "transitive" | "qip-element", source: "\<vertex ID\>"

7\. id: \<ID\>, type: "mandatory" | "optional" | "startup", protocol: "http", reference: "transitive", source: "\<COMPONENT_ID\>"

9a\. id: \<ID\>, type: "consumer", name: \<TopicName\>

15\. id: \<ID\>, type: "mandatory" | "optional"

Outgoing Edges:

2\. See in the list of incoming edges

3\. id: \<ID\>, type: "module", component: \<ComponentId\>

4\. id: \<ID\>, type: "library"

5\. id: \<ID\>, type: "info"

6\. id: \<ID\>, type: "implemented"

7\. See in the list of incoming edges

8\. id: \<ID\>, type: "mandatory" | "optional", viaZookeeper: "yes" | "no" | "not required"

9b\. id: \<ID\>, type: "producer", name: \<TopicName\>

10\. id: \<ID\>, type: "errorCode"

11\. id: \<ID\>, type: "directory"

12\. id: \<ID\>, type: "file"

13\. id: \<ID\>, type: "defines" | "implements"

14\. id: \<ID\>, type: "qip-call"

![schema](https://uml.planttext.com/plantuml/png/VPDTQuCm58RlyojogR2wskwZAoBTT1s52Ilq3nmrs51D94QwCVRVTwBT6CpaBX_FUtAIzyscqw1qtzI4T0NKZCw35I6A5Hf4XTDFuYLG7BceVEAbe267Zk5m9qhs3ffBuPClWbko0IvyEfl5r8loE95DAmKJEhhEOtzDlH15ZsrRys9KqI1maF_MxOKNL_JUhiXasTVCS4kVJ5C6sofes5cgao5lxELUyLLwg8Q2jnRCS2-WsHa-N1XIXsnhY3iaihtZYW_WE0gL1ibjrqFanq7aLlPkVjrrOt-S7q5xQyzAIKKJMGurmxcZnPIRp4arF5beYy1W5H_z31Uke_UxDreSCP8qEZobOwwMZgfJlCc1MMSrQn2_x5m4ayb5hyc6YHB_qpS0)

[Source](https://www.planttext.com/?text=VPDTQuCm58RlyojogR2wskwZAoBTT1s52Ilq3nmrs51D94QwCVRVTwBT6CpaBX_FUtAIzyscqw1qtzI4T0NKZCw35I6A5Hf4XTDFuYLG7BceVEAbe267Zk5m9qhs3ffBuPClWbko0IvyEfl5r8loE95DAmKJEhhEOtzDlH15ZsrRys9KqI1maF_MxOKNL_JUhiXasTVCS4kVJ5C6sofes5cgao5lxELUyLLwg8Q2jnRCS2-WsHa-N1XIXsnhY3iaihtZYW_WE0gL1ibjrqFanq7aLlPkVjrrOt-S7q5xQyzAIKKJMGurmxcZnPIRp4arF5beYy1W5H_z31Uke_UxDreSCP8qEZobOwwMZgfJlCc1MMSrQn2_x5m4ayb5hyc6YHB_qpS0)

#### Data Model {#component-data-model}

| Property                                                      | Type     | Mandatory | Description                                                                                                                 |
|:--------------------------------------------------------------|:---------|:----------|:----------------------------------------------------------------------------------------------------------------------------|
| id                                                            | String   | yes       | Unique ID                                                                                                                   |
| type                                                          | String   | yes       | One of the following values:"backend", "library", "ui", "ui app bundle", "ui backend", "ui cdn", "config", "job", "unknown" |
| name                                                          | String   | yes       | Component name                                                                                                              |
| repository                                                    | String   | no        | Git repository (URL)                                                                                                        |
| directoryPath                                                 | String   | no        | Path to the local directory where tool stores temporary data for this Component                                             |
| openApiSpecPath                                               | String   | no        | Path to the local directory where tool stores openApi spec for this Component                                               |
| abbreviation                                                  | String   | yes       | Component abbreviation                                                                                                      |
| features                                                      | Object   | no        | Supported or not features                                                                                                   |
| features/hpa                                                  | Object   | no        |                                                                                                                             |
| features/hpa/fileLink                                         | String   | yes       | Git-repository file link                                                                                                    |
| features/hpa/resource                                         | String   | yes       |                                                                                                                             |
| features/hpa/scaleup                                          | Boolean  | yes       |                                                                                                                             |
| features/hpa/scaledown                                        | Boolean  | yes       |                                                                                                                             |
| features/hpa/variables                                        | String[] | yes       | Empty list by default                                                                                                       |
| features/blueGreen                                            | Object   | no        |                                                                                                                             |
| features/blueGreen/httpRequest                                | String   | yes       | One of: yes, no, not required                                                                                               |
| features/blueGreen/httpCallback                               | String   | yes       | One of: yes, no, not required                                                                                               |
| features/blueGreen/zeebeWorkers                               | String   | yes       | Deprecated. One of: yes, no, not required                                                                                   |
| features/blueGreen/messageQueueConsumers                      | String   | yes       | One of: yes, no, not required                                                                                               |
| features/blueGreen/messageQueueProducers                      | String   | yes       | One of: yes, no, not required                                                                                               |
| features/multitenancy                                         | Object   | no        |                                                                                                                             |
| features/multitenancy/defaultTenantId                         | String   | yes       | One of: yes, no, not required                                                                                               |
| features/multitenancy/database                                | String   | yes       | One of: yes, no, not required                                                                                               |
| features/multitenancy/messageQueue                            | String   | yes       | One of: yes, no, not required                                                                                               |
| features/multitenancy/elasticsearch                           | String   | yes       | One of: yes, no, not required                                                                                               |
| features/multitenancy/caching                                 | String   | yes       | One of: yes, no, not required                                                                                               |
| features/multitenancy/logging                                 | String   | yes       | One of: yes, no, not required                                                                                               |
| features/multitenancy/multithreading                          | String   | yes       | One of: yes, no, not required                                                                                               |
| features/faultTolerance                                       | Object   | no        |                                                                                                                             |
| features/faultTolerance/errorCodes                            | String   | yes       | One of: yes, no, not required                                                                                               |
| features/faultTolerance/httpRetryPolicy                       | String   | yes       | One of: yes, no, not required                                                                                               |
| features/faultTolerance/customHealthProbes                    | String   | yes       | One of: yes, no, not required                                                                                               |
| details                                                       | Object   | no        | Additional details for this Component                                                                                       |
| details/name                                                  | String   | yes       |                                                                                                                             |
| details/owner                                                 | String   | yes       |                                                                                                                             |
| details/abbreviation                                          | String   | yes       |                                                                                                                             |
| details/dnsName                                               | String   | yes       |                                                                                                                             |
| details/dnsNames                                              | String[] | yes       | Since version 2 of graph model                                                                                              |
| details/domain                                                | String   | yes       |                                                                                                                             |
| details/description                                           | String   | no        |                                                                                                                             |
| details/type                                                  | String   | yes       |                                                                                                                             |
| details/stickySessions                                        | Boolean  | no        |                                                                                                                             |
| details/reactive                                              | Boolean  | no        |                                                                                                                             |
| details/framework                                             | String   | no        |                                                                                                                             |
| details/language                                              | String   | no        |                                                                                                                             |
| details/tmfSpec                                               | String[] | no        |                                                                                                                             |
| details/serviceMonitorEnabled                                 | boolean  | yes       |                                                                                                                             |
| details/passwords                                             | String[] | no        |                                                                                                                             |
| details/smartPlugPlugins                                      | String[] | no        |                                                                                                                             |
| details/documentationLink                                     | String[] | no        |                                                                                                                             |
| details/applicationConfiguration                              | Object   | no        |                                                                                                                             |
| details/dockerfile                                            | Object   | no        |                                                                                                                             |
| details/dockerfile/imageRoot                                  | String   | yes       |                                                                                                                             |
| details/database                                              | Object   | no        |                                                                                                                             |
| details/database/database                                     | Object[] | yes       |                                                                                                                             |
| details/database/database[]/item                              | String   | yes       | Database name & version                                                                                                     |
| details/database/database[]/viaZookeeper                      | String   | yes       | Value: yes, no, not required                                                                                                |
| details/database/externalIndices[]/item                       | String   | yes       |                                                                                                                             |
| details/database/externalIndices[]/viaZookeeper               | String   | yes       | Value: yes, no, not required                                                                                                |
| details/database/externalCache                                | Object[] | yes       |                                                                                                                             |
| details/database/externalCache[]/item                         | String   | yes       |                                                                                                                             |
| details/database/externalCache[]/viaZookeeper                 | String   | yes       | Value: yes, no, not required                                                                                                |
| details/messageQueues                                         | Object   | yes       |                                                                                                                             |
| details/messageQueues/rabbitMQ                                | Object   | yes       |                                                                                                                             |
| details/messageQueues/rabbitMQ/producer                       | String[] | no        |                                                                                                                             |
| details/messageQueues/rabbitMQ/consumer                       | String[] | no        |                                                                                                                             |
| details/messageQueues/kafka                                   | Object   | yes       |                                                                                                                             |
| details/messageQueues/kafka/producer                          | String[] | no        |                                                                                                                             |
| details/messageQueues/kafka/consumer                          | String[] | no        |                                                                                                                             |
| details/dependencies                                          | Object   | yes       |                                                                                                                             |
| details/dependencies/startup                                  | String[] | no        |                                                                                                                             |
| details/dependencies/mandatory                                | String[] | no        |                                                                                                                             |
| details/dependencies/optional                                 | String[] | no        |                                                                                                                             |
| details/api                                                   | Object   | yes       |                                                                                                                             |
| details/api/openApi                                           | String   | no        | Value: yes, no, not required                                                                                                |
| details/api/apiSpecPublished                                  | String   | no        | Value: yes, no, not required                                                                                                |
| details/api/apiVersioning                                     | String   | no        | Value: yes, no, not required                                                                                                |
| details/deployments                                           | Object   | yes       |                                                                                                                             |
| details/deployments/<file_name>                               | Object   | no        |                                                                                                                             |
| details/deployments/<file_name>/JAVA_OPTIONS                  | String   | no        |                                                                                                                             |
| details/deployments/<file_name>/CPU_REQUEST                   | String   | no        |                                                                                                                             |
| details/deployments/<file_name>/CPU_LIMIT                     | String   | no        |                                                                                                                             |
| details/deployments/<file_name>/MEMORY_LIMIT                  | String[] | no        |                                                                                                                             |
| details/deployments/<file_name>/MEMORY_REQUEST                | String[] | no        |                                                                                                                             |
| details/deployments/<file_name>/javaOptions                   | Object   | no        |                                                                                                                             |
| details/deploymentConfiguration                               | Object   | no        |                                                                                                                             |
| details/deploymentConfiguration/deployOptions                 | Object   | no        |                                                                                                                             |
| details/deploymentConfiguration/deployOptions/generateGateway | Boolean  | yes       |                                                                                                                             |
| details/deploymentConfiguration/deployOptions/bluegreen       | Boolean  | yes       |                                                                                                                             |
| details/k8s-labels                                            | Object[] | no        | Kubernetes labels of microservice                                                                                           |
| details/k8s-labels/fileLink                                   | String   | no        | Git URL of YAML file                                                                                                        |
| details/k8s-labels/kind                                       | String   | no        | Kind of deployment descriptor                                                                                               |
| details/k8s-labels/labels                                     | Object[] | no        | Labels theirselves                                                                                                          |
| details/k8s-labels/labels/name                                | String   | no        | Label name                                                                                                                  |
| details/k8s-labels/labels/value                               | String   | no        | Label value                                                                                                                 |

### QIP chain (TBD)
Qubership Integration Platform: integration chain

Incoming Edges:

1\. id: \<ID\>, type: "implements" | "defines"
2\. id: \<ID\>, type: "qip-call"

Outgoing Edges:

2\. id: \<ID\>, type: "includes"

![schema](https://uml.planttext.com/plantuml/png/SoWkIImgAStDuU8gJaxCILKmqBFWuj9mpiyjoCzBpIlHLD3LjLFGi4co2_GCK73EICpCiz5FijEqKj0mr2HEdH5G7OOuH3b8p3KdDHTa0CH4BeVKl1IWSm00)

[Source](https://www.planttext.com/?text=SoWkIImgAStDuU8gJaxCILKmqBFWuj9mpiyjoCzBpIlHLD3LjLFGi4co2_GCK73EICpCiz5FijEqKj0mr2HEdH5G7OOuH3b8p3KdDHTa0CH4BeVKl1IWSm00)

#### Data Model {#qip-chain-data-model}

| Property    | Type   | Mandatory | Description                             |
|:------------|:-------|:----------|:----------------------------------------|
| id          | String | yes       | Chain ID (usually with "chain-" prefix) |
| type        | String | yes       | Value is "qip-chain"                    |
| fileLink    | String | yes       | Git URL of definition (YAML file)       |
| description | String | no        | Description from definition             |

#### QIP Element (TBD)
Qubership Integration Platform: integration chain element

Incoming Edges:

1\. id: \<ID\>, type: "includes"

2\. id: \<ID\>, type: "qip-consumer", name: <topic name>

Outgoing Edges:

3\. id: \<ID\>, type: "optional" | "mandatory"

4\. id: \<ID\>, type: "qip-producer", name: <topic name>

![schema](https://uml.planttext.com/plantuml/png/SoWkIImgAStDuU8gJaxCILKmqBFWuj88z0nGICv8pCpJLD3LjLFGi4co0mcvvgJcfkQLsEWdsMbQAMWOQd9f-0RYLM84L8951gB5EJ-t83yFA0PIQOnF9T0Me18JJIukXzIy5A1p0G00)

[Source](https://www.planttext.com/?text=SoWkIImgAStDuU8gJaxCILKmqBFWuj88z0nGICv8pCpJLD3LjLFGi4co0mcvvgJcfkQLsEWdsMbQAMWOQd9f-0RYLM84L8951gB5EJ-t83yFA0PIQOnF9T0Me18JJIukXzIy5A1p0G00)

#### Data Model {#qip-element-data-model}

| Property    | Type   | Mandatory | Description                                      |
|:------------|:-------|:----------|:-------------------------------------------------|
| id          | String | yes       | Element ID (usually with "qip-" prefix)          |
| type        | String | yes       | Value is "qip-element"                           |
| elementType | String | yes       | Element type in QIP chain definition             |
| name        | String | no        | Element name from definition                     |
| protocol    | String | no        | "http"/"Kafka"/"RabbitMQ"                        |
| direction   | String | no        | "producer"/"consumer"                            |
| topic       | String | no        | Topic name for Kafka or RabbitMQ                 |
| url         | String | no        | Called URL, with possible variables placeholders |
| service     | String | no        | DNS name of called microservice                  |

### Error Code

Error Code that can be returned by the Component.

Incoming Edges:

1\. id: \<ID\>, type: "errorCode"

Outgoing Edges: not provided

![schema](https://uml.planttext.com/plantuml/png/SoWkIImgAStDuU8gJaxCILKmqBFWuj9mpiyjoCzBpIlHLD3LjLFGi4coSoqgoYzISCvFIRNHJxBJj5BGCDJaud98pKi1wGO0)

[Source](https://www.planttext.com/?text=SoWkIImgAStDuU8gJaxCILKmqBFWuj9mpiyjoCzBpIlHLD3LjLFGi4coSoqgoYzISCvFIRNHJxBJj5BGCDJaud98pKi1wGO0)

#### Data Model {#error-code-data-model}

| Property | Type   | Mandatory | Description                    |
|:---------|:-------|:----------|:-------------------------------|
| id       | String | yes       | Error Code                     |
| type     | String | yes       | Value is "errorCode"           |
| name     | String | yes       | Error Code                     |

### Module
One of the Component's artifacts.

Incoming Edges:

1\. id: \<ID\>, type: "module", component: <ComponentId>

Outgoing Edges:

2\. id: \<ID\>, type: "dependency", scope: "test" | "compile" | "provided" | "runtime", component: <ComponentId>

![schema](https://uml.planttext.com/plantuml/png/SoWkIImgAStDuU8gJaxCILKmqBFWuj9mpiyjoCzBpIlHLD3LjLFGi4coyyrFASr9jT5FijEqKj0mrEH24uGezCbCAaeigWGfCTBaud98pKi16Gu0)

[Source](https://www.planttext.com/?text=SoWkIImgAStDuU8gJaxCILKmqBFWuj9mpiyjoCzBpIlHLD3LjLFGi4coyyrFASr9jT5FijEqKj0mrEH24uGezCbCAaeigWGfCTBaud98pKi16Gu0)

#### Data Model {#module-data-model}

| Property   | Type   | Mandatory | Description                               |
|:-----------|:-------|:----------|:------------------------------------------|
| id         | String | yes       | Unique ID                                 |
| type       | String | yes       | Value is "library"                        |
| package    | String | yes       | Value is "pom" for Maven based Components |
| groupId    | String | yes       | Group ID                                  |
| artifactId | String | yes       | Artifact ID                               |
| version    | String | yes       | Module Version                            |

### Library
Component library dependencies.

Incoming Edges:

1\. id: \<ID\>, type: "dependency", scope: "test" | "compile" | "provided" | "runtime", component: <ComponentId>

Outgoing Edges:

2\. id: \<ID\>, type: "dependency", scope: "test" | "compile" | "provided" | "runtime", component: <ComponentId>

![schema](https://uml.planttext.com/plantuml/png/SoWkIImgAStDuU8gJaxCILKmqBFWujBmpKyfpKdLLD3LjLFGi4coyybCAaeighJHJxBJj5BGCDJaGXV5gLH7mKYJYyiXDIy5P0i0)

[Source](https://www.planttext.com/?text=SoWkIImgAStDuU8gJaxCILKmqBFWujBmpKyfpKdLLD3LjLFGi4coyybCAaeighJHJxBJj5BGCDJaGXV5gLH7mKYJYyiXDIy5P0i0)

#### Data Model {#library-data-model}

| Property       | Type   | Mandatory | Description                                           |
|:---------------|:-------|:----------|:------------------------------------------------------|
| id             | String | yes       | Unique ID                                             |
| type           | String | yes       | Value is "library"                                    |
| package        | String | yes       | Value is "jar" for Java based Components              |
| groupId        | String | yes       | Group ID                                              |
| artifactId     | String | yes       | Artifact ID                                           |
| version        | String | yes       | Library Version                                       |

### Infrastructure
Root vertex for all types of DB, MQ and other 3-rd party stateful deployment units.

Incoming edges:

1\. id: \<ID\>

Outgoing edges:

2\. id: \<ID\>

![schema](https://uml.planttext.com/plantuml/png/SoWkIImgAStDuU8gJaxCILKmqBFWuj88oiylqLJGrRLJqB19ilFCIojAB2ufAaqkAIrAjT5FijEqKj0mrEJ28GdHw99OafYKM9mAKcj4GArdNafgHM99PduUCQgT4vCpClFIYL7g6mXLnUMGcfS2yYS0)

[Source](https://www.planttext.com/?text=SoWkIImgAStDuU8gJaxCILKmqBFWuj88oiylqLJGrRLJqB19ilFCIojAB2ufAaqkAIrAjT5FijEqKj0mrEJ28GdHw99OafYKM9mAKcj4GArdNafgHM99PduUCQgT4vCpClFIYL7g6mXLnUMGcfS2yYS0)

#### Data model {#infrastructure-data-model}

| Property          | Type   | Mandatory | Description               |
|:------------------|:-------|:----------|:--------------------------|
| id                | String | yes       | Unique ID                 |
| type              | String | yes       | Value is "infrastructure" |
| name              | String | yes       | Value is "Infrastructure" |
| technical-manager | String | yes       | Technical manager name    |

### Database
Incoming edges:

1\. id: \<ID\>

2\. id: \<ID\>, type: "mandatory" | "optional", viaZookeeper: "yes" | "no" | "not required"

Outgoing edges: not provided

![schema](https://uml.planttext.com/plantuml/png/SoWkIImgAStDuU8gJaxCILKmqBFWujBmp4ihIYmkAIfDBYajIjLKqDMrKz2mIR9pIIn9J4eiJhNHJxBJj5BGCDJaqd3Epot8pqlDAy6jp4YJYyiXDIy5P0y0)

[Source](https://www.planttext.com/?text=SoWkIImgAStDuU8gJaxCILKmqBFWujBmp4ihIYmkAIfDBYajIjLKqDMrKz2mIR9pIIn9J4eiJhNHJxBJj5BGCDJaqd3Epot8pqlDAy6jp4YJYyiXDIy5P0y0)

#### Data model {#database-data-model}

| Property          | Type   | Mandatory | Description               |
|:------------------|:-------|:----------|:--------------------------|
| id                | String | yes       | Unique ID                 |
| type              | String | yes       | Value is "database"       |
| name              | String | yes       | Database name & version   |

### Indexation
Input edges:

1\. id: \<ID\>

2\. id: \<ID\>, type: "mandatory" | "optional", viaZookeeper: "yes" | "no" | "not required"

Output edges: not provided

![schema](https://uml.planttext.com/plantuml/png/SoWkIImgAStDuU8gJaxCILKmqBFWujBmp4ihIYmkAIfDBYajIjLKqDMrKz2mIRBpp4j9hKWioSpFiz5FijEqKj0mrEJISCxFBSZFIyqhmQVGI9EBoo4rBmLa5m00)

[Source](https://www.planttext.com/?text=SoWkIImgAStDuU8gJaxCILKmqBFWujBmp4ihIYmkAIfDBYajIjLKqDMrKz2mIRBpp4j9hKWioSpFiz5FijEqKj0mrEJISCxFBSZFIyqhmQVGI9EBoo4rBmLa5m00)

#### Data model {#indexation-data-model}

| Property | Type   | Mandatory | Description              |
|:---------|:-------|:----------|:-------------------------|
| id       | String | yes       | Unique ID                |
| type     | String | yes       | Value is "indexation"    |
| name     | String | yes       | Name & version           |

### Caching
Incoming edges:

1\.id: \<ID\>, type: "mandatory" | "optional", viaZookeeper: "yes" | "no" | "not required"

Output edges: not provided

![schema](https://uml.planttext.com/plantuml/png/SoWkIImgAStDuU8gJaxCILKmqBFWujBmp4ihIYmkAIfDBYajIjLKqDMrKz2mIR9pJapEoCnBjz5FijEqKj0mrEJISCxFBSZFIyqhmQdAI9EBoo4rBmLa2m00)

[Source](https://www.planttext.com/?text=SoWkIImgAStDuU8gJaxCILKmqBFWujBmp4ihIYmkAIfDBYajIjLKqDMrKz2mIR9pJapEoCnBjz5FijEqKj0mrEJISCxFBSZFIyqhmQdAI9EBoo4rBmLa2m00)

#### Data model {#caching-data-model}

| Property | Type   | Mandatory | Description              |
|:---------|:-------|:----------|:-------------------------|
| id       | String | yes       | Unique ID                |
| type     | String | yes       | Value is "caching"       |
| name     | String | yes       | Cache name & version     |

### MQ
Message queue provider (e.g.: "Kafka")

Incoming edges:

1\. id: \<ID\>

2\. id: \<ID\>, type: "producer", name: <TopicName>, reference: "transitive" | "qip-element" | <none>, source: <ComponentId>

4\. id: \<ID\>, type: "qip-producer", name: <TopicName>

Outgoing edges:

3\. id: \<ID\>, type: "consumer", name: \<TopicName\>, reference: "transitive" | "qip-element" | <none>, source: <ComponentId>

5\. id: \<ID\>, type: "qip-consumer", name: \<TopicName\>

![schema](https://uml.planttext.com/plantuml/png/SoWkIImgAStDuU8gJaxCILKmqBFWujBmp4ihIYmkAIfDBYajIjLKqDMrKz2mIRBp3RJHJxBJj5BGCDJaqd3Epot8pqlDAy6cm0Ye8D0pGC4r9pKNXn8Je1A404G1maoW0cCi2f3D12enrUJYSaZDIm5v0G00)

[Source](https://www.planttext.com/?text=SoWkIImgAStDuU8gJaxCILKmqBFWujBmp4ihIYmkAIfDBYajIjLKqDMrKz2mIRBp3RJHJxBJj5BGCDJaqd3Epot8pqlDAy6cm0Ye8D0pGC4r9pKNXn8Je1A404G1maoW0cCi2f3D12enrUJYSaZDIm5v0G00)

#### Data model {#message-queue-data-model}

| Property             | Type     | Mandatory | Description             |
|:---------------------|:---------|:----------|:------------------------|
| id                   | String   | yes       | Unique ID               |
| type                 | String   | yes       | Value is "mq"           |
| name                 | String   | yes       | MQ name & version       |
| drivers              | Object[] | no        | Known driver libraries  |
| drivers[]/groupId    | String   | no        | Driver library group    |
| drivers[]/artifactId | String   | no        | Driver library artifact |

### Information
Root vertex for all non deployment units for general information about Components.

Input edges:

1\. id: \<ID\>

Output edges:

2\. id: \<ID\>

![schema](https://uml.planttext.com/plantuml/png/SoWkIImgAStDuU8gJaxCILKmqBFWuj88oiylqLJGrRLJqB19ilFCIyiloaqioSpFiz5FijEqKj0mrEJ29WFHubQKc9jQdb-K3L9fX4-bJs9UUcbYUYf1XUw99QdbYPLGTGwfUIaWDmC0)

[Source](https://www.planttext.com/?text=SoWkIImgAStDuU8gJaxCILKmqBFWuj88oiylqLJGrRLJqB19ilFCIyiloaqioSpFiz5FijEqKj0mrEJ29WFHubQKc9jQdb-K3L9fX4-bJs9UUcbYUYf1XUw99QdbYPLGTGwfUIaWDmC0)

#### Data model {#information-data-model}

| Property          | Type   | Mandatory | Description              |
|:------------------|:-------|:----------|:-------------------------|
| id                | String | yes       | Value is "Info"          |
| type              | String | yes       | Value is "Information"   |
| name              | String | yes       | Value is "Information"   |
| technical-manager | String | yes       | Value is "none"          |

### Framework

Input edges:

1\. id: \<ID\>

2\. id: \<ID\>, type: "info"

Output edges: not provided

![schema](https://uml.planttext.com/plantuml/png/SoWkIImgAStDuU8gJaxCILKmqBFWujBmp4lBByfDBCdCpzDKqDMrKz2mIR9pAqhCJItFBygsqK-oqxHIq33KvD9mpiyjoCzBpIl1eyv8aulB8JKl1MGD0000)

[Source](https://www.planttext.com/?text=SoWkIImgAStDuU8gJaxCILKmqBFWujBmp4lBByfDBCdCpzDKqDMrKz2mIR9pAqhCJItFBygsqK-oqxHIq33KvD9mpiyjoCzBpIl1eyv8aulB8JKl1MGD0000)

#### Data model {#framework-data-model}

| Property | Type   | Mandatory | Description          |
|:---------|:-------|:----------|:---------------------|
| id       | String | yes       | Unique ID            |
| type     | String | yes       | Value is "framework" |
| name     | String | yes       | Framework name       |

### Language

Input edges:

1\. id: \<ID\>

2\. id: \<ID\>, type: "info", usage: ["source", "target", "etc."]

Output edges: not provided

![schema](https://uml.planttext.com/plantuml/png/SoWkIImgAStDuU8gJaxCILKmqBFWujBmp4lBByfDBCdCpzDKqDMrKz2mIRBpISnBBqrCJxNHJxBJj5BGCDJaqd3Epot8pqlDAy6jp4YJYyiXDIy5P0a0)

[Source](https://www.planttext.com/?text=SoWkIImgAStDuU8gJaxCILKmqBFWujBmp4lBByfDBCdCpzDKqDMrKz2mIRBpISnBBqrCJxNHJxBJj5BGCDJaqd3Epot8pqlDAy6jp4YJYyiXDIy5P0a0)

#### Data model {#language-data-model}

| Property | Type   | Mandatory | Description                         |
|:---------|:-------|:----------|:------------------------------------|
| id       | String | yes       | Unique ID (language name + version) |
| type     | String | yes       | Value is "language"                 |
| name     | String | yes       | Normalized Language name            |
| version  | String | no        | Language version                    |
  
### Gateway

Input edges:

1\. id: \<ID\>

2\. id: \<ID\>, type: "info"

Output edges: not provided

![schema](https://uml.planttext.com/plantuml/png/SoWkIImgAStDuU8gJaxCILKmqBFWujBmp4lBByfDBCdCpzDKqDMrKz2mIR9pJon9BK-ijD5FijEqKj0mrEJISCxFBSZFIyqhmQdAI9EBoo4rBmLa1G00)

[Source](https://www.planttext.com/?text=SoWkIImgAStDuU8gJaxCILKmqBFWujBmp4lBByfDBCdCpzDKqDMrKz2mIR9pJon9BK-ijD5FijEqKj0mrEJISCxFBSZFIyqhmQdAI9EBoo4rBmLa1G00)

#### Data model {#gateway-data-model}

| Property | Type   | Mandatory | Description                                |
|:---------|:-------|:----------|:-------------------------------------------|
| id       | String | yes       | Unique ID                                  |
| type     | String | yes       | Value is "gateway"                         |
| name     | String | yes       | Type of the gateway supported by component |

### Specification
Root vertex for all specification that Components implements.

Input edges:

1\. id: \<ID\>

Output edges:

2\. id: \<ID\>

![schema](https://uml.planttext.com/plantuml/png/SoWkIImgAStDuU8gJaxCILKmqBFWuj88oiylqLJGrRLJqB19iWikI4tEJCjCJYp9pC-pqK-oqxHIq33KvC8k1z4Muki6KcMaoSNba9gN0h810000)

[Source](https://www.planttext.com/?text=SoWkIImgAStDuU8gJaxCILKmqBFWuj88oiylqLJGrRLJqB19iWikI4tEJCjCJYp9pC-pqK-oqxHIq33KvC8k1z4Muki6KcMaoSNba9gN0h810000)

#### Data model {#specification-data-model}

| Property          | Type   | Mandatory | Description              |
|:------------------|:-------|:----------|:-------------------------|
| id                | String | yes       | Value is "Spec"          |
| type              | String | yes       | Value is "Specification" |
| name              | String | yes       | Value is "Specification" |
| technical-manager | String | yes       | Value is "none"          |

### TMF

This vertex represents one of the TMF specification.

Input edges:

1\. id: \<ID\>

2\. id: \<ID\>, type: "implemented"

Output edges: not provided

![schema](https://uml.planttext.com/plantuml/png/SoWkIImgAStDuU8gJaxCILKmqBFWuj88BaXDpapBJ4uioSpFqrJGrRLJqB19iWlnTRFHJxBJj5BGCDJaqd3Epot8pqlDAy6gmaYJYyiXDIy5P3W0)

[Source](https://www.planttext.com/?text=SoWkIImgAStDuU8gJaxCILKmqBFWuj88BaXDpapBJ4uioSpFqrJGrRLJqB19iWlnTRFHJxBJj5BGCDJaqd3Epot8pqlDAy6gmaYJYyiXDIy5P3W0)

#### Data model {#tmf-data-model}

| Property | Type   | Mandatory | Description                                            |
|:---------|:-------|:----------|:-------------------------------------------------------|
| id       | String | yes       | TMF specification code                                 |
| type     | String | yes       | Value is "tmf"                                         |
| name     | String | yes       | TMF specification name                                 |
| url      | String | no        | URL to the TMF specification official description page |
| version  | String | no        | TMF specification version                              |
| code     | String | yes       | TMF specification code                                 |

### File

This vertex represents one file from component Git-repository (configuration file, for example).

Input edges:

1\. id: \<ID\>, type: "file"

2\. id: \<ID\>, type: "file"

3\. id: \<ID\>, type: "directory"

4\. id: \<ID\>, type: "directory"

Output edges: none provided

![schema](https://uml.planttext.com/plantuml/png/SoWkIImgAStDuU8gJaxCILKmqBFWuj9mpiyjoCzBpIlHLD3LjLFGi4coSylCIRNHJxBJj5BGCDJ4LEAIMPIQN99VL0bIO0nKW130ei04Ym8qIumqkRWSKlDIG8u30000)

[Source](https://www.planttext.com/?text=SoWkIImgAStDuU8gJaxCILKmqBFWuj9mpiyjoCzBpIlHLD3LjLFGi4coSylCIRNHJxBJj5BGCDJ4LEAIMPIQN99VL0bIO0nKW130ei04Ym8qIumqkRWSKlDIG8u30000)

#### Data model {#file-data-model}

| Property   | Type   | Mandatory | Description                    |
|:-----------|:-------|:----------|:-------------------------------|
| id         | String | yes       | Unique ID                      |
| type       | String | yes       | Value is "file" or "directory" |
| path       | String | yes       | Path                           |
| name       | String | yes       | Filename                       |
| fileLink   | String | no        | Git-repository URL             |
| content    | String | no        | Original content               |
| structured | JSON   | no        | Parsed content                 |

<!-- textlint-enable terminology -->
