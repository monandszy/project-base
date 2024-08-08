-- init.sql
CREATE DATABASE app_template_dev;
CREATE DATABASE app_template;
CREATE DATABASE flashcard_dev;
CREATE DATABASE flashcard;

\c app_template_dev
CREATE SCHEMA app;

\c app_template
CREATE SCHEMA app;

\c flashcard_dev
CREATE SCHEMA app;

\c flashcard
CREATE SCHEMA app;