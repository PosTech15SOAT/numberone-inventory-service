-- Execute como postgres somente na instância descartável de I1.
CREATE ROLE inventory_i1 LOGIN PASSWORD 'i1-local-inventory';
CREATE ROLE os_i1 LOGIN PASSWORD 'i1-local-os';
CREATE DATABASE inventory_db OWNER inventory_i1;
CREATE DATABASE service_order_db OWNER os_i1;

REVOKE CONNECT ON DATABASE inventory_db FROM PUBLIC;
REVOKE CONNECT ON DATABASE service_order_db FROM PUBLIC;
GRANT CONNECT ON DATABASE inventory_db TO inventory_i1;
GRANT CONNECT ON DATABASE service_order_db TO os_i1;
