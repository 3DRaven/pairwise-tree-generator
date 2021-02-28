# Pairwise-tree-generator

Library for generate trees (as example JSONs) from base description based on [pairwiser](https://github.com/3DRaven/pairwiser) library

# Example

We have WEB API with input json. So, lets write description of possible variants of generated JSONs.
We can describe field names and possible field values (objects can be values too):

```json
{
    "properties": {
        "minRestrictions": [
            {
                "fieldPath": "fieldD",
                "value": 1
            }
        ],
        "maxRestrictions": [
            {
                "fieldPath": "fieldD",
                "value": 2
            }
        ],
        "objects": [
            "fieldC"
        ]
    },
    "source": {
        "fieldA": [
            "Avalue1"
        ],
        "fieldB": [
            null
        ],
        "fieldC": [
            {
                "subFieldA": [
                    1
                ]
            }
        ],
        "fieldD": [
            {
                "subFieldA": [
                    1
                ]
            }
        ]
    }
}
```

Let's get generated JSONs variants from description:

```java
//Get description Map
Map<String,Object> generationDescription = mapper.readValue(sourceDescriptionJson,new TypeReference<Map<String,Object>>() {});
//Get Properties for generation from source json (as example)
Properties properties = mapper.convertValue(generationDescription.get("properties"),Properties.class);
//Get description of possible field values
Map<String,List<Object>> jsonsDescription = mapper.convertValue(generationDescription.get("source"),new TypeReference<Map<String,List<Object>>>() {});
//Let's generate
final PairwiseJsonGenerator gen = new PairwiseJsonGenerator();
//Generated list of possible variants of JSONs
final List<Map<String, Object>> generated = gen.generate(jsonsDescription, properties);
```

## Final cases (converted from tree of objects to json):

```
[
    {
        "fieldA": "Avalue1",
        "fieldC": null,
        "fieldB": null,
        "fieldD": [
            {
                "subFieldA": 1
            }
        ]
    },
    {
        "fieldA": "Avalue1",
        "fieldC": {
            "subFieldA": 1
        },
        "fieldB": null,
        "fieldD": [
            {
                "subFieldA": 1
            }
        ]
    }
]
```
# License

Apache License
Version 2.0, January 2004
http://www.apache.org/licenses/
