-- Inspiration: http://contrib.spip.net/Export-Spip-vers-Wordpress (mais très incomplet)

-- http://codex.wordpress.org/Function_Reference/wp_upload_dir
UPDATE @PREFIX@options
SET option_value='@UPLOAD_DIR@'
WHERE option_name='upload_path';

-- Imports authors (only one real author, so just adjust admin user)
UPDATE @PREFIX@users
SET
    user_login='Jebegood',
    user_nicename='Jebegood',
    display_name='Jebegood',
    user_email='tribalstaff@gmail.com'
WHERE ID=1;

-- Imports categories (from rubriques)
REPLACE INTO @PREFIX@terms (
    term_id,
    name,
    slug,
    term_group
)
SELECT
    id_rubrique,
    titre,
    CONCAT("rub",id_rubrique),
    1
FROM
    @SPIP_DB@.spip_rubriques;
REPLACE INTO @PREFIX@term_taxonomy (
    term_taxonomy_id,
    term_id,
    taxonomy,
    parent,
    description
)
SELECT
    id_rubrique,
    id_rubrique,
    'category',
    0,
    ''
FROM
    @SPIP_DB@.spip_rubriques;

-- Update category urls
UPDATE @PREFIX@terms, @SPIP_DB@.spip_urls
SET slug = @SPIP_DB@.spip_urls.url
WHERE @SPIP_DB@.spip_urls.id_objet = term_id
AND @SPIP_DB@.spip_urls.type = "rubrique";

-- Import posts (articles in spip)
REPLACE INTO @PREFIX@posts (
    ID
    , post_author
    , post_date, post_date_gmt
    , post_excerpt
    , post_content
    , post_content_filtered
    , post_title
    , post_name
    , to_ping , pinged
    , post_modified,
    post_modified_gmt,
    post_status
)
SELECT
    p.id_article
    , 1
    , p.date, p.date
    , p.descriptif
    , concat(p.chapo, p.texte)
    , ''
    , titre
    , COALESCE(REPLACE(l.url, ' ', ''), CONCAT('art', p.id_article))
    , '', ''
    , p.date_modif,
    p.date_modif,
    CASE p.statut WHEN 'prepa' THEN 'draft' WHEN 'prop' THEN 'pending' WHEN 'publie' THEN 'publish' WHEN 'refuse' THEN 'trash' END
FROM
  @SPIP_DB@.spip_articles AS p
  LEFT JOIN @SPIP_DB@.spip_urls AS l ON l.id_objet = p.id_article AND l.type = 'article';
 
-- Import news (breves in spip)
REPLACE INTO @PREFIX@posts (
    ID
    , post_author
    , post_date,
    post_date_gmt
    , post_excerpt
    , post_content
    , post_content_filtered
    , post_title
    , post_name
    , to_ping,
    pinged
    , post_modified,
    post_modified_gmt,
    post_status,
    post_type
)
SELECT
    b.id_breve + @NEWS_ID_OFFSET@,
    1,
    b.date_heure,
    b.date_heure,
    '',
    b.texte,
    '',
    titre,
    COALESCE(REPLACE(l.url, ' ', ''), CONCAT('breve', b.id_breve)),
    '',
    '',
    b.date_heure,
    b.date_heure,
    CASE b.statut WHEN 'prepa' THEN 'draft' WHEN 'prop' THEN 'pending' WHEN 'publie' THEN 'publish' WHEN 'refuse' THEN 'trash' END,
    'news'
FROM
  @SPIP_DB@.spip_breves AS b
  LEFT JOIN @SPIP_DB@.spip_urls AS l ON l.id_objet = b.id_breve AND l.type = 'breve';

-- Link posts to terms
REPLACE INTO @PREFIX@term_relationships (
    object_id,
    term_taxonomy_id
)
SELECT
    p.id_article,
    p.id_rubrique
FROM
    @SPIP_DB@.spip_articles AS p;

-- Link news to terms
REPLACE INTO @PREFIX@term_relationships (
    object_id,
    term_taxonomy_id
)
SELECT
    b.id_breve + @NEWS_ID_OFFSET@,
    b.id_rubrique
FROM
    @SPIP_DB@.spip_breves AS b;

-- Update all the counts for the categories
UPDATE @PREFIX@term_taxonomy tt
SET count=(SELECT COUNT(1) FROM @PREFIX@term_relationships rel WHERE rel.term_taxonomy_id = tt.term_taxonomy_id);

-- Import images and other attached files (documents in spip)
INSERT INTO @PREFIX@posts (
    ID,
    post_author,
    post_date,
    post_date_gmt,
    post_excerpt,
    post_content,
    post_content_filtered,
    post_title,
    to_ping,
    pinged,
    post_modified,
    post_modified_gmt,
    post_type,
    GUID,
    post_mime_type,
    post_status
)
SELECT
    d.id_document + @ATTACHMENT_ID_OFFSET@,
    1,
    d.date,
    d.date,
    d.descriptif,
    d.descriptif,
    '',
    d.titre,
    '',
    '',
    d.date,
    d.date,
    'attachment',
    CONCAT('IMG/', d.fichier),
    CASE d.extension WHEN 'jpg' THEN 'image/jpeg' WHEN 'png' THEN 'image/png' WHEN 'gif' THEN 'image/gif' WHEN 'pdf' THEN 'application/pdf' WHEN 'zip' THEN 'application/zip' WHEN 'xls' THEN 'application/vnd.ms-excel' WHEN 'doc' THEN 'application/msword' WHEN 'mp3' THEN 'audio/mpeg' WHEN 'bmp' THEN 'image/x-ms-bmp' WHEN 'docx' THEN 'application/vnd.openxmlformats-officedocument.wordprocessingml.document' WHEN 'xlsx' THEN 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet' END,
    'private'
FROM
  @SPIP_DB@.spip_documents d
WHERE
  d.extension != 'html';

INSERT INTO @PREFIX@postmeta (
    meta_id,
    post_id,
    meta_key,
    meta_value
)
SELECT
    d.id_document + @ATTACHMENT_ID_OFFSET@,
    d.id_document + @ATTACHMENT_ID_OFFSET@,
    '_wp_attached_file',
    d.fichier
FROM
  @SPIP_DB@.spip_documents d
WHERE
  d.extension != 'html';

-- Import comments (forum in spip)
REPLACE INTO @PREFIX@comments (
      comment_ID
    , comment_post_ID
    , comment_author
    , comment_author_email
    , comment_author_url
    , comment_date
    , comment_date_gmt
    , comment_content
    , comment_parent
    , comment_approved
)
SELECT
      id_forum
    , id_article
    , auteur
    , email_auteur
    , url_site
    , date_heure
    , date_heure
    , texte
    , id_parent
    , 1
FROM
    @SPIP_DB@.spip_forum
WHERE
    statut = 'publie';

-- Update comments numbers per post
UPDATE @PREFIX@posts p
SET p.comment_count = (SELECT COUNT(1) FROM @PREFIX@comments c WHERE c.comment_post_ID = p.ID);
 
-- Update the syntax. Basically transform weird SPIP stuff into HTML
update @PREFIX@posts set post_content = replace(post_content, '{{{', ' <h1> ') where instr(post_content, '{{{') > 0;
update @PREFIX@posts set post_content = replace(post_content, '}}}', ' </h1> ') where instr(post_content, '}}}') > 0;
update @PREFIX@posts set post_content = replace(post_content, '{{', ' <b> ') where instr(post_content, '{{') > 0;
update @PREFIX@posts set post_content = replace(post_content, '}}', ' </b> ') where instr(post_content, '}}') > 0;
update @PREFIX@posts set post_content = replace(post_content, '{', ' <i> ') where instr(post_content, '{') > 0;
update @PREFIX@posts set post_content = replace(post_content, '}', ' </i> ') where instr(post_content, '}') > 0;
update @PREFIX@posts set post_content = replace(post_content, '[[', ' <blockquote> ') where instr(post_content, '[[') > 0;
update @PREFIX@posts set post_content = replace(post_content, ']]', ' </blockquote> ') where instr(post_content, ']]') > 0;
update @PREFIX@posts set post_content = replace(post_content, '*]', ' </strong></i> ') where instr(post_content, '*]') > 0;
update @PREFIX@posts set post_content = replace(post_content, '[*', ' </strong></i> ') where instr(post_content, '*]') > 0;