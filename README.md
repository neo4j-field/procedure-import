# User defined procedure for data loading

## Use case

This is an example of a simple use case where we need to load a `CSV` file containing information on communication between 
different phone numbers where we have the data of communication and the type of communication (SMS or CALL).

## Why a user defined procedure?

If you need to implement specific business rules to validate or transform the data you are loading, then it's a way to 
consider.

When we choose to go on a User `Defined Procedure approach`, we will write `Java` code that can be unit tested easily.

This code can be checked on every commit in your CI pipeline, which adds confidence and quality to your overall projects.

For this use case, we need to validate every entry to check if:

- All the fields are present
- The date field matches a given date format
- The phone numbers are well-defined and respect the international phone format
- The types are known (CALL/SMS)

This can be achieved directly through Cypher, but with long query, if/else hacks which complexify the queries.

For this example, we have a `CSVColsValidator` which is tested by `CSVColsValidatorTest`.

## Parallelism and batching

We can define how many threads can be used to load the data. We can pass them in parallel and make some assertions on, for 
example, if we try to use more than the available cores on the machine, we can fallback directly on the available cores 
so we can maximize the processing.

In plus, the batches can be defined to minimize the commits and maximize the performance. But this should be set carefully 
depending on the available allocated memory.