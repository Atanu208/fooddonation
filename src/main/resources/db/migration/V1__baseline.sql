-- V1: Baseline - captures the exact schema produced by the original
-- Hibernate-generated database so Flyway can take over change management.

CREATE TABLE donations (
    id bigint NOT NULL,
    completed_at timestamp(6) without time zone,
    created_at timestamp(6) without time zone,
    expiry_time character varying(255),
    food_description character varying(255),
    food_type character varying(255),
    is_packaged boolean NOT NULL,
    pickup_address character varying(255),
    pickup_city character varying(255),
    pickup_pincode character varying(255),
    pickup_state character varying(255),
    pickup_time timestamp(6) without time zone NOT NULL,
    quantity character varying(255),
    special_instructions character varying(255),
    status character varying(255),
    updated_at timestamp(6) without time zone,
    donor_id bigint NOT NULL,
    ngo_id bigint,
    version bigint,
    CONSTRAINT donations_status_check CHECK (((status)::text = ANY ((ARRAY['PENDING'::character varying, 'ACCEPTED'::character varying, 'PICKED_UP'::character varying, 'DELIVERED'::character varying, 'COMPLETED'::character varying, 'CANCELLED'::character varying])::text[])))
);

CREATE SEQUENCE donations_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

ALTER SEQUENCE donations_id_seq OWNED BY donations.id;
ALTER TABLE ONLY donations ALTER COLUMN id SET DEFAULT nextval('donations_id_seq'::regclass);
ALTER TABLE ONLY donations ADD CONSTRAINT donations_pkey PRIMARY KEY (id);

CREATE TABLE notifications (
    id bigint NOT NULL,
    created_at timestamp(6) without time zone NOT NULL,
    donation_id bigint,
    is_read boolean NOT NULL,
    message character varying(1000),
    title character varying(200) NOT NULL,
    type character varying(255) NOT NULL,
    user_id bigint NOT NULL,
    CONSTRAINT notifications_type_check CHECK (((type)::text = ANY ((ARRAY['DONATION_CREATED'::character varying, 'DONATION_ACCEPTED'::character varying, 'DONATION_STATUS_CHANGED'::character varying, 'DONATION_CANCELLED'::character varying, 'DONATION_EXPIRED'::character varying, 'SYSTEM'::character varying])::text[])))
);

CREATE SEQUENCE notifications_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

ALTER SEQUENCE notifications_id_seq OWNED BY notifications.id;
ALTER TABLE ONLY notifications ALTER COLUMN id SET DEFAULT nextval('notifications_id_seq'::regclass);
ALTER TABLE ONLY notifications ADD CONSTRAINT notifications_pkey PRIMARY KEY (id);

CREATE TABLE users (
    id bigint NOT NULL,
    address character varying(255),
    city character varying(255),
    created_at timestamp(6) without time zone,
    email character varying(255) NOT NULL,
    is_active boolean NOT NULL,
    name character varying(100),
    organization_name character varying(255),
    password character varying(255),
    phone_number character varying(255),
    pincode character varying(255),
    role character varying(255),
    state character varying(255),
    updated_at timestamp(6) without time zone
);

CREATE SEQUENCE users_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

ALTER SEQUENCE users_id_seq OWNED BY users.id;
ALTER TABLE ONLY users ALTER COLUMN id SET DEFAULT nextval('users_id_seq'::regclass);
ALTER TABLE ONLY users ADD CONSTRAINT users_pkey PRIMARY KEY (id);
ALTER TABLE ONLY users ADD CONSTRAINT uk_6dotkott2kjsp8vw4d0m25fb7 UNIQUE (email);

CREATE INDEX idx_notification_read ON notifications USING btree (is_read);
CREATE INDEX idx_notification_user ON notifications USING btree (user_id);

ALTER TABLE ONLY notifications
    ADD CONSTRAINT fk9y21adhxn0ayjhfocscqox7bh FOREIGN KEY (user_id) REFERENCES users(id);
ALTER TABLE ONLY donations
    ADD CONSTRAINT fkca1lvywwrw987q70hm4sq5mmw FOREIGN KEY (ngo_id) REFERENCES users(id);
ALTER TABLE ONLY donations
    ADD CONSTRAINT fkp8lwp38vg4a0v2y69d2krn562 FOREIGN KEY (donor_id) REFERENCES users(id);
