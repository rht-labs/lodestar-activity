# LodeStar Activity

This project provides activity data for LodeStar.

The API is document via swagger and is available at `/q/swagger-ui`

An experimental GraphQL deployment is available at `/q/graphql-ui` and `graphql/schema.graphql`

----

## Configuration

The following environment variables are available:

### Logging
| Name | Default | Description|
|------|---------|------------|
| ENGAGEMENT_API_URL | http://git-api:8080 | The url too get engagement data |
| GITLAB_API_URL | https://acmegit.com | The url to Gitlab |
| GITLAB_TOKEN | t | The Access Token for Gitlab |
| WEBHOOK_TOKEN | t | Shared secret for Gitlab Webhooks | 
| LODESTAR_LOGGING | DEBUG | Logging to the base source package | 
| COMMIT_FILTERED_MESSAGE_LIST | anual_refresh | A list of messages to filter from activity |
| COMMIT_FILTERED_EMAIL_LIST | bot@bot.com | A list of emails to filter from activity | 
| POSTGRESQL_USER | | The db user | 
| POSTGRESQL_PASSWORD | | The db password |
| POSTGRESQL_URL | | The jdbc url to the db |

## Deployment

See the deployment [readme](./deployment) for information on deploying to a OpenShift environment

## Running the application locally

### Postgresql 

A postgres database is needed for development. To spin up a docker postresql container run the following

```
cd deployment
docker-compose up
```

### Local Dev

You can run your application in dev mode that enables live coding using:

```
export GITLAB_API_URL=https://gitlab.com/ 
export GITLAB_TOKEN=token
export ENGAGEMENT_API_URL=https://git-api.test.com 
mvn quarkus:dev
```

> **_NOTE:_**  Quarkus now ships with a Dev UI, which is available in dev mode only at http://localhost:8080/q/dev/.
