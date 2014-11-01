-- First, drop everything
DROP DATABASE IF EXISTS @SPIP_DB@;
DROP DATABASE IF EXISTS @WP_DB@;
GRANT USAGE ON *.* TO 'tbz'@'%';
DROP USER 'tbz'@'%';

-- Then, recreate everything cleanly
CREATE DATABASE @SPIP_DB@ CHARACTER SET utf8 COLLATE utf8_general_ci;
CREATE DATABASE @WP_DB@ CHARACTER SET utf8 COLLATE utf8_general_ci;
GRANT ALL ON @WP_DB@.* TO 'tbz'@'%' IDENTIFIED BY 'tbzpwd';
