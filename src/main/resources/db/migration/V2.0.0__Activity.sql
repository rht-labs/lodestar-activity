
DROP TABLE IF EXISTS Activity;
DROP TABLE IF EXISTS Commit;

create table Activity (
    id varchar(255) not null,
    authorEmail varchar(255),
    authorName varchar(255),
    authoredDate timestamp,
    committedDate timestamp,
    committerEmail varchar(255),
    committerName varchar(255),
    engagementUuid varchar(255),
    message TEXT,
    projectId int8,
    region varchar(255),
    shortId varchar(255),
    title varchar(255),
    url varchar(255),
    primary key (id)
    );

DROP INDEX IF EXISTS project_index;
DROP INDEX IF EXISTS subdomain_index;
DROP INDEX IF EXISTS engagement_uuid_index;
DROP INDEX IF EXISTS region_index;
DROP SEQUENCE IF EXISTS hibernate_sequence;

create index project_index on Activity (projectId);
create index engagement_uuid_index on Activity (engagementUuid);
create index committed_date_index on Activity (committedDate);
create index region_index on Activity (region, committedDate);

create sequence hibernate_sequence start 1 increment 1;