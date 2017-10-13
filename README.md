# recommender

A simple redis based recommender engine, derived from redis-labs go example

## Installation

Install dependencies with lein

## Usage

Currently only an API. Run in REPL

Add Rating
```clojure
(rate "user-name" "item-name" rating)
```

Update Database
```clojure
(update-db max-similar-users)
```

Suggest
```clojure
(suggest "users-name" max-suggestions)
```


