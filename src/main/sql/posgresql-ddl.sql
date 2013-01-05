--
-- PostgreSQL database dump
--

SET statement_timeout = 0;
SET client_encoding = 'UTF8';
SET standard_conforming_strings = on;
SET check_function_bodies = false;
SET client_min_messages = warning;

SET search_path = public, pg_catalog;

SET default_tablespace = '';

SET default_with_oids = false;

--
-- Name: apache_user; Type: TABLE; Schema: public; Owner: esper; Tablespace: 
--

CREATE TABLE apache_user (
    id character varying(100),
    name character varying(100)
);


ALTER TABLE public.apache_user OWNER TO esper;

--
-- Name: build_seq; Type: SEQUENCE; Schema: public; Owner: esper
--

CREATE SEQUENCE build_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE public.build_seq OWNER TO esper;

--
-- Name: build; Type: TABLE; Schema: public; Owner: esper; Tablespace: 
--

CREATE TABLE build (
    uuid character(36) NOT NULL,
    created_date timestamp without time zone NOT NULL,
    seq integer DEFAULT nextval('build_seq'::regclass) NOT NULL,
    "timestamp" timestamp without time zone NOT NULL,
    success boolean NOT NULL,
    reference_type character varying(100) NOT NULL,
    reference_uuid character(36) NOT NULL
);


ALTER TABLE public.build OWNER TO esper;

--
-- Name: build_participant; Type: TABLE; Schema: public; Owner: esper; Tablespace: 
--

CREATE TABLE build_participant (
    build character(36) NOT NULL,
    person character(36) NOT NULL
);


ALTER TABLE public.build_participant OWNER TO esper;

--
-- Name: file; Type: TABLE; Schema: public; Owner: esper; Tablespace: 
--

CREATE TABLE file (
    uuid character(36) NOT NULL,
    created_date timestamp without time zone NOT NULL,
    url character varying(1000) NOT NULL,
    content_type character varying(100) NOT NULL,
    data bytea
);


ALTER TABLE public.file OWNER TO esper;

--
-- Name: jenkins_build_seq; Type: SEQUENCE; Schema: public; Owner: esper
--

CREATE SEQUENCE jenkins_build_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE public.jenkins_build_seq OWNER TO esper;

--
-- Name: jenkins_build; Type: TABLE; Schema: public; Owner: esper; Tablespace: 
--

CREATE TABLE jenkins_build (
    uuid character(36) NOT NULL,
    created_date timestamp without time zone NOT NULL,
    seq integer DEFAULT nextval('jenkins_build_seq'::regclass) NOT NULL,
    job character(36) NOT NULL,
    file character(36) NOT NULL,
    entry_id character varying(1000) NOT NULL,
    url character varying(1000) NOT NULL,
    result character varying(100),
    number integer,
    duration integer,
    "timestamp" timestamp without time zone,
    users character(36)[]
);


ALTER TABLE public.jenkins_build OWNER TO esper;

--
-- Name: jenkins_job; Type: TABLE; Schema: public; Owner: esper; Tablespace: 
--

CREATE TABLE jenkins_job (
    uuid character(36) NOT NULL,
    created_date timestamp without time zone NOT NULL,
    server character(36) NOT NULL,
    file character(36) NOT NULL,
    url character varying(1000) NOT NULL,
    job_type character varying(100) NOT NULL,
    display_name character varying(100)
);


ALTER TABLE public.jenkins_job OWNER TO esper;

--
-- Name: jenkins_server; Type: TABLE; Schema: public; Owner: esper; Tablespace: 
--

CREATE TABLE jenkins_server (
    uuid character(36) NOT NULL,
    created_date timestamp without time zone NOT NULL,
    url character varying(1000) NOT NULL,
    enabled boolean NOT NULL
);


ALTER TABLE public.jenkins_server OWNER TO esper;

--
-- Name: jenkins_user; Type: TABLE; Schema: public; Owner: esper; Tablespace: 
--

CREATE TABLE jenkins_user (
    uuid character(36) NOT NULL,
    created_date timestamp without time zone NOT NULL,
    server character(36) NOT NULL,
    absolute_url character varying(1000) NOT NULL
);


ALTER TABLE public.jenkins_user OWNER TO esper;

--
-- Name: person; Type: TABLE; Schema: public; Owner: esper; Tablespace: 
--

CREATE TABLE person (
    uuid character(36) NOT NULL,
    created_date timestamp without time zone NOT NULL,
    name character varying(100),
    mail character varying(100)
);


ALTER TABLE public.person OWNER TO esper;

--
-- Name: person_badge; Type: TABLE; Schema: public; Owner: esper; Tablespace: 
--

CREATE TABLE person_badge (
    uuid character(36) NOT NULL,
    created_date timestamp without time zone NOT NULL,
    person character(36) NOT NULL,
    name character varying(100) NOT NULL,
    level integer NOT NULL,
    count integer NOT NULL
);


ALTER TABLE public.person_badge OWNER TO esper;

--
-- Name: person_badge_progress; Type: TABLE; Schema: public; Owner: esper; Tablespace: 
--

CREATE TABLE person_badge_progress (
    uuid character(36) NOT NULL,
    created_date timestamp without time zone NOT NULL,
    person character(36) NOT NULL,
    badge character varying(100) NOT NULL,
    state character varying(8000) NOT NULL
);


ALTER TABLE public.person_badge_progress OWNER TO esper;

--
-- Name: person_jenkins_user; Type: TABLE; Schema: public; Owner: esper; Tablespace: 
--

CREATE TABLE person_jenkins_user (
    person character(36) NOT NULL,
    jenkins_user character(36) NOT NULL
);


ALTER TABLE public.person_jenkins_user OWNER TO esper;

--
-- Name: table_poller_status; Type: TABLE; Schema: public; Owner: esper; Tablespace: 
--

CREATE TABLE table_poller_status (
    poller_name character varying(100) NOT NULL,
    last_seq integer NOT NULL,
    last_run timestamp without time zone,
    duration integer,
    status character varying(1000)
);


ALTER TABLE public.table_poller_status OWNER TO esper;

--
-- Name: pk_build; Type: CONSTRAINT; Schema: public; Owner: esper; Tablespace: 
--

ALTER TABLE ONLY build
    ADD CONSTRAINT pk_build PRIMARY KEY (uuid);


--
-- Name: pk_build_participant; Type: CONSTRAINT; Schema: public; Owner: esper; Tablespace: 
--

ALTER TABLE ONLY build_participant
    ADD CONSTRAINT pk_build_participant PRIMARY KEY (build, person);


--
-- Name: pk_file; Type: CONSTRAINT; Schema: public; Owner: esper; Tablespace: 
--

ALTER TABLE ONLY file
    ADD CONSTRAINT pk_file PRIMARY KEY (uuid);


--
-- Name: pk_jenkins_build; Type: CONSTRAINT; Schema: public; Owner: esper; Tablespace: 
--

ALTER TABLE ONLY jenkins_build
    ADD CONSTRAINT pk_jenkins_build PRIMARY KEY (uuid);


--
-- Name: pk_jenkins_job; Type: CONSTRAINT; Schema: public; Owner: esper; Tablespace: 
--

ALTER TABLE ONLY jenkins_job
    ADD CONSTRAINT pk_jenkins_job PRIMARY KEY (uuid);


--
-- Name: pk_jenkins_server; Type: CONSTRAINT; Schema: public; Owner: esper; Tablespace: 
--

ALTER TABLE ONLY jenkins_server
    ADD CONSTRAINT pk_jenkins_server PRIMARY KEY (uuid);


--
-- Name: pk_jenkins_user; Type: CONSTRAINT; Schema: public; Owner: esper; Tablespace: 
--

ALTER TABLE ONLY jenkins_user
    ADD CONSTRAINT pk_jenkins_user PRIMARY KEY (uuid);


--
-- Name: pk_job_status; Type: CONSTRAINT; Schema: public; Owner: esper; Tablespace: 
--

ALTER TABLE ONLY table_poller_status
    ADD CONSTRAINT pk_job_status PRIMARY KEY (poller_name);


--
-- Name: pk_person; Type: CONSTRAINT; Schema: public; Owner: esper; Tablespace: 
--

ALTER TABLE ONLY person
    ADD CONSTRAINT pk_person PRIMARY KEY (uuid);


--
-- Name: pk_person_badge; Type: CONSTRAINT; Schema: public; Owner: esper; Tablespace: 
--

ALTER TABLE ONLY person_badge
    ADD CONSTRAINT pk_person_badge PRIMARY KEY (uuid);


--
-- Name: pk_person_badge_progress; Type: CONSTRAINT; Schema: public; Owner: esper; Tablespace: 
--

ALTER TABLE ONLY person_badge_progress
    ADD CONSTRAINT pk_person_badge_progress PRIMARY KEY (uuid);


--
-- Name: pk_person_jenkins_user; Type: CONSTRAINT; Schema: public; Owner: esper; Tablespace: 
--

ALTER TABLE ONLY person_jenkins_user
    ADD CONSTRAINT pk_person_jenkins_user PRIMARY KEY (person, jenkins_user);


--
-- Name: uq_jenkins_build__id; Type: CONSTRAINT; Schema: public; Owner: esper; Tablespace: 
--

ALTER TABLE ONLY jenkins_build
    ADD CONSTRAINT uq_jenkins_build__id UNIQUE (entry_id);


--
-- Name: uq_jenkins_build__seq; Type: CONSTRAINT; Schema: public; Owner: esper; Tablespace: 
--

ALTER TABLE ONLY jenkins_build
    ADD CONSTRAINT uq_jenkins_build__seq UNIQUE (seq);


--
-- Name: uq_jenkins_job__url; Type: CONSTRAINT; Schema: public; Owner: esper; Tablespace: 
--

ALTER TABLE ONLY jenkins_job
    ADD CONSTRAINT uq_jenkins_job__url UNIQUE (url);


--
-- Name: uq_jenkins_server__url; Type: CONSTRAINT; Schema: public; Owner: esper; Tablespace: 
--

ALTER TABLE ONLY jenkins_server
    ADD CONSTRAINT uq_jenkins_server__url UNIQUE (url);


--
-- Name: uq_jenkins_user__absolute_url; Type: CONSTRAINT; Schema: public; Owner: esper; Tablespace: 
--

ALTER TABLE ONLY jenkins_user
    ADD CONSTRAINT uq_jenkins_user__absolute_url UNIQUE (absolute_url);


--
-- Name: uq_person_badge__person__name__level; Type: CONSTRAINT; Schema: public; Owner: esper; Tablespace: 
--

ALTER TABLE ONLY person_badge
    ADD CONSTRAINT uq_person_badge__person__name__level UNIQUE (person, name, level);


--
-- Name: uq_person_badge_progress__person_badge; Type: CONSTRAINT; Schema: public; Owner: esper; Tablespace: 
--

ALTER TABLE ONLY person_badge_progress
    ADD CONSTRAINT uq_person_badge_progress__person_badge UNIQUE (person, badge);


--
-- Name: ix_jenkins_build__created_date; Type: INDEX; Schema: public; Owner: esper; Tablespace: 
--

CREATE INDEX ix_jenkins_build__created_date ON jenkins_build USING btree (created_date);


--
-- Name: fk_build_participant__build; Type: FK CONSTRAINT; Schema: public; Owner: esper
--

ALTER TABLE ONLY build_participant
    ADD CONSTRAINT fk_build_participant__build FOREIGN KEY (build) REFERENCES build(uuid);


--
-- Name: fk_build_participant__person; Type: FK CONSTRAINT; Schema: public; Owner: esper
--

ALTER TABLE ONLY build_participant
    ADD CONSTRAINT fk_build_participant__person FOREIGN KEY (person) REFERENCES person(uuid);


--
-- Name: fk_jenkins_build__file; Type: FK CONSTRAINT; Schema: public; Owner: esper
--

ALTER TABLE ONLY jenkins_build
    ADD CONSTRAINT fk_jenkins_build__file FOREIGN KEY (file) REFERENCES file(uuid);


--
-- Name: fk_jenkins_build__job; Type: FK CONSTRAINT; Schema: public; Owner: esper
--

ALTER TABLE ONLY jenkins_build
    ADD CONSTRAINT fk_jenkins_build__job FOREIGN KEY (job) REFERENCES jenkins_job(uuid);


--
-- Name: fk_jenkins_job__file; Type: FK CONSTRAINT; Schema: public; Owner: esper
--

ALTER TABLE ONLY jenkins_job
    ADD CONSTRAINT fk_jenkins_job__file FOREIGN KEY (file) REFERENCES file(uuid);


--
-- Name: fk_jenkins_job__server; Type: FK CONSTRAINT; Schema: public; Owner: esper
--

ALTER TABLE ONLY jenkins_job
    ADD CONSTRAINT fk_jenkins_job__server FOREIGN KEY (server) REFERENCES jenkins_server(uuid);


--
-- Name: fk_jenkins_user__server; Type: FK CONSTRAINT; Schema: public; Owner: esper
--

ALTER TABLE ONLY jenkins_user
    ADD CONSTRAINT fk_jenkins_user__server FOREIGN KEY (server) REFERENCES jenkins_server(uuid);


--
-- Name: fk_person_badge__person; Type: FK CONSTRAINT; Schema: public; Owner: esper
--

ALTER TABLE ONLY person_badge
    ADD CONSTRAINT fk_person_badge__person FOREIGN KEY (person) REFERENCES person(uuid);


--
-- Name: fk_person_badge_progress__person; Type: FK CONSTRAINT; Schema: public; Owner: esper
--

ALTER TABLE ONLY person_badge_progress
    ADD CONSTRAINT fk_person_badge_progress__person FOREIGN KEY (person) REFERENCES person(uuid);


--
-- Name: fk_person_jenkins_user__jenkins_user; Type: FK CONSTRAINT; Schema: public; Owner: esper
--

ALTER TABLE ONLY person_jenkins_user
    ADD CONSTRAINT fk_person_jenkins_user__jenkins_user FOREIGN KEY (jenkins_user) REFERENCES jenkins_user(uuid);


--
-- Name: fk_person_jenkins_user__person; Type: FK CONSTRAINT; Schema: public; Owner: esper
--

ALTER TABLE ONLY person_jenkins_user
    ADD CONSTRAINT fk_person_jenkins_user__person FOREIGN KEY (person) REFERENCES person(uuid);


--
-- Name: public; Type: ACL; Schema: -; Owner: postgres
--

REVOKE ALL ON SCHEMA public FROM PUBLIC;
REVOKE ALL ON SCHEMA public FROM postgres;
GRANT ALL ON SCHEMA public TO postgres;
GRANT ALL ON SCHEMA public TO PUBLIC;


--
-- PostgreSQL database dump complete
--

