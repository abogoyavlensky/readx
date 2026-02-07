# readx

_This application is generated with [clojure-stack-spa](https://github.com/abogoyavlensky/clojure-stack-spa)._

_TODO: add project description_


## Development

Install Java, Clojure, Babashka, Node and other tools manually or via [mise](https://mise.jdx.dev/):

```shell
mise trust && mise install
```

First, run JS and CSS build in watch mode for local development:

```shell
bb ui-dev
```

JS and CSS assets will be served from `http://localhost:8000/assets/` and will be automatically rebuilt on changes.

In a separate terminal run server from built-in REPL:

> [!NOTE]
> If you're using PostgreSQL, [Docker](https://docs.docker.com/engine/install/) should be installed

 ```shell
bb clj-repl 
(reset)
````

The server should be available at `http://localhost:8000`.

## Project management

Check all available commands:

```shell
bb tasks
```

Run lint, formatting and tests:

```shell
bb check
```

Check outdated dependencies:

```shell
bb outdated
```

## Deployment

For detailed deployment instructions, refer to the documentation:

- [Kamal](https://stack.bogoyavlensky.com/docs/spa/kamal)
