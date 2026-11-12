-- Execute como postgres somente na instância descartável de I2.
CREATE ROLE inventory_i2 LOGIN PASSWORD 'i2-local-inventory';
CREATE ROLE os_i2 LOGIN PASSWORD 'i2-local-os';
CREATE DATABASE inventory_db OWNER inventory_i2;
CREATE DATABASE service_order_db OWNER os_i2;

REVOKE CONNECT ON DATABASE inventory_db FROM PUBLIC;
REVOKE CONNECT ON DATABASE service_order_db FROM PUBLIC;
GRANT CONNECT ON DATABASE inventory_db TO inventory_i2;
GRANT CONNECT ON DATABASE service_order_db TO os_i2;
