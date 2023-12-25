## Home assignment for Fashion Digital (via BilgeAdam)

### Exercise: Political Speeches
(mostly copied from the assignment `PDF`)

The goal of this exercise is to calculate some statistics from given input data about political speeches. The application should handle CSV files ( encoded), structured as below:

```
Redner, Thema, Datum, Wörter
Alexander Abel, Bildungspolitik, 2012-10-30, 5310
Bernhard Belling, Kohlesubventionen, 2012-11-05, 1210
Caesare Collins, Kohlesubventionen, 2012-11-06, 1119
Alexander Abel, Innere Sicherheit, 2012-12-11, 911
```
English translation: "Redner" (column 0) means speaker, "Thema" (column 1) translates to topic, "Datum" (column 2) is the date of a speech and the last column "Wörter" holds the word count of the speech.

The example file can be downloaded at https://fid-recruiting.s3-eu-west-1.amazonaws.com/politics.csv.

**Note: this `CSV` file appears to have additional spaces after commas, which is not how `CSV` files should be structured. That's why the application trims them whenever possible.**

The application should provide an HTTP endpoint which accepts one or more given URLs (http and https) via query parameters at the path: `GET /evaluation?url=url1&url=url2`. The provided csv files at these URLs should be downloaded, processed and evaluated to answer the following questions:

- Which politician gave the most speeches in 2013?
- Which politician gave the most speeches on the topic "Innere Sicherheit"?
- Which politician used the fewest words?

**Note: there is no indication if the same speech can be in separate `CSV` files, and how to identify those occasions, if they exist. Therefore, the application treats all those speeches as different.**

The answers should be provided as `JSON`. If a question cannot be answered or does not have an unambiguous solution the result for this field should be `null`. As an example for the given input above the expected result is:

```
{
    "mostSpeeches": null,
    "mostSecurity": "Alexander Abel",
    "leastWordy": "Caesare Collins"
}
```

**Note: `JSON` specification makes it clear that specific layout is NOT considered significant, which is why the application actually outputs everything in one line.**

### Usage

```
sbt run
```

This application is a normal `sbt` project. It can be compiled with `sbt compile` or run with `sbt run`. Additionally, a self-contained `jar` file can be created with `sbt assembly`

The language used is Scala `3.3.1`, which fits with the requirement that the version should be `>= 2.12`. It can be, of course, easily rewritten for Scala 2, if necessary.

The application, when launched (either by `sbt run` or by running a `jar` file created by `sbt assembly`) opens two ports: `8080` for plain `HTTP`, and `8081` for `HTTPS`. The latter uses a self-signed certificate (included in the project), and so requires the `-k` key if tested with `curl`:

```
curl -k 'https://localhost:8081/evaluation?url=https://fid-recruiting.s3-eu-west-1.amazonaws.com/politics.csv'
```

Plain `HTTP` works as well:

```
curl 'http://localhost:8080/evaluation?url=https://fid-recruiting.s3-eu-west-1.amazonaws.com/politics.csv'
```

There was no requirement to support HTTPS, but it was fun to do.

If any of the URLs provided in the query are invalid or can't be resolved or do not resolve to a `CSV` file, the application reports an error and does not output the result of processing the rest. This is deliberate: otherwise the user might accidentally get incorrect result and not realise that in time.

It's also possible that `CSV` files would be served with a wrong `Content-Type`, so the application does not check for it being `text/csv`, but rather examines the content.

The application can be stopped by hitting `return` in the terminal where it runs.
