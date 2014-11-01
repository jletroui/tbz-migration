Apperçu de l'outil
==================

Importe:

- Les articles (avec les liens, les images au bon endroit, à la bonne taille, etc...) (deviennent des 'posts')
- Les rubriques (deviennent des 'categories')
- Les images et autres documents attachés aux posts (pdf, mp3, etc...) (deviennent des 'attachments')
- Les commentaires
- Les brêves (deviennent des 'news')

N'importe pas:

- Les auteurs
- Les référencements
- Les sites syndiqués
- Les mots
- L'alignement des images 'lightbox'. Ces images là sont centrées.

Plan de migration
=================

Voici une idée générale de comment tu vas pouvoir travailler pour développer la nouvelle version du site, puis migrer pour vrai.

Phase de développement du squelette
-----------------------------------

Alors pendant cette phase, tu travailles en local sur ton ordinateur de travail. Tu bosses sur le squelette, les plugins et la configuration Wordpresss dans un Wordpress normal, vide. 
Ensuite, lorsque tu es prêt à vouloir vérifier avec le contenu de TribalZine, tu lances la migration dans une nouvelle base de donnée:

- D'abord, fais un export de ta base de donnée Wordpress de travail (celle ou tu as installer les plugins, le thème, etc...)
- Tu lances la migration à partir de cet export dans une nouvelle base de donéé
- Tu modifie la config Wordpress (wp-config.php) pour pointer sur la base migrée
- Tu mattes le résultat, notes les trucs à changer
- IMPORTANT: tu re modifie la config Wordpress pour pointer sur la base de donnée Wordpress de travail sans contenu
- Tu fais les changements sur le thème, les plugins, etc...
- Puis on recommence jusqu'à ce que le résultat soit nickel.

C'est important de ne travailler que sur un Wordpress vide, car lors de la migration, il faudra partir d'un Wordpress vide (mais configuré avec plugins, le squelette, etc..).

Phase de migration du vrai site
-------------------------------

Le jour J, préparation:

- On arrête de modifier le site Spip. Plus d'éditions jusqu'à ce que la migration soit complète.
- On fait un export de la BD spip dans le répertoire de migration
- On récupère un export de la dernière version de la BD Wordpress de travail (sur l'ordinateur de travail)
- On lance la migration en local sur l'ordinateur de travail, avec la configuration finale (en particulier le paramètre `website_url`)
- On exporte la BD migrée
- On l'importe dans le MySQL du site web (une nouvelle BD, pas celle de Spip)

A ce point ci, l'ancien site Spip est encore en ligne et accessible. Là commence la partie ou l'on va devoir être 'hors ligne' pendant qq minutes.

- On ajoute un fichier `index.html` mitonné à la main annonçant en fanfare que le site web est en cours de migration. 
- On ajoute un fichier `htaccess.txt` qui redirige n'importe quelle URL autre que 'index.html' vers 'index.html'. Lorsque l'on va sur 'www.tribalzine.com', on ne voit que le index.html.
- On supprimes tous les fichiers spip, cad tout sauf le répertoire `IMG`, le `htaccess.txt`, `index.html` et ses qq images.
- On upload les fichiers Wordpress
- On mets à jour le `wp-config.php` du site web pour pointer vers la nouvelle BD
- On remet un `htaccess.txt` normal
- On vérifie que le site web est tout beau
- On supprime `index.html`

Et zou! C'est fait! Normalement, le site web n'aura été innaccessible que quelques minutes, le gros du temps aura été d'uploader les fichiers wordpress.

Utilisation de l'outil de migration
===================================

Installation et préparation
---------------------------

Il y a pas mal de trucs à installer sur l'ordinateur qui va servir à développer la nouvelle version de tbz.

- Décompresser le fichier zip qq part sur l'ordinateur (déjà fait puisque tu lis ce README...). Appelons ce répertoire le répertoire de migration pour la suite.
- Installer Java Runtime Environment 1.7
- Installer MySQL et PHP
- Installer Wordpress localement qq part et noter le nom de la base de donnée de travail dans MySQL (par exemple: tbz_dev)
- Copier le répertoire `IMG` de TribalZine à la racine du site Wordpress. Attention, c'est énorme, ca m'a pris 3 jours pour télécharger ca (~ 60 Go). Conseil: utiliser [rsync](https://www.digitalocean.com/community/tutorials/how-to-use-rsync-to-sync-local-and-remote-directories-on-a-vps), histoire de n'avoir qu'à synchroniser les nouvelles images régulièrement sans se retaper le téléchargement au complet. 
- Faire un export de la base de donnée Spip du site TribalZine au format .sql dans le répertoire de migration (par exemple: `tbz_spip.sql`)
- Ouvrir le fichier `migration.conf` et ajuster les paramètres si besoin. Probablement le seul paramètre à changer est `website_url`.
- Ouvrir une ligne de commande dans le répertoire de migration

Voilà, après tout ça, on est prêt à migrer autant de fois que l'on veut.

Vérifier l'installation
-----------------------

Lancer la commande suivante:

    >java -jar migrate-tbz.jar

Devrais afficher le mode d'emploi de l'outil:

    Tribal Zine DB migration tool from Spip to Wordpress
    
    Usage: java -jar migrate-tbz.jar <source sql export> <destination sql export>
      <source sql export> database export of TribalZine spip website
      <destination sql export> database export of Wordpress website after having installed and configured plugins, theme, etc...
    
    Example: java -jar migrate-tbz.jar tbz_export_2014-09-08.sql wordpress_fresh_noplugins.sql
    
Sinon, cela veut probablement dire que Java 1.7 n'est pas correctement installé. La commande suivante doit bien indiquer que la version 1.7 est installée:
    
    >java -version
    java version "1.7.0_51"
    Java(TM) SE Runtime Environment (build 1.7.0_51-b13)
    Java HotSpot(TM) 64-Bit Server VM (build 24.51-b03, mixed mode)

Explication des fichiers
------------------------

- `README.md`: la documentation que tu es en train de lire
- `migrate-tbz.conf`: la configuration de la migration
- `migrate-tbz.jar`: l'outil de migration
- `wordpress_fresh_noplugins.sql`: exemple d'export d'un site Wordpress de travail complètement vide
- `tbz_export_2014-09-08.sql`: exemple d'export de la BD Spip de TribalZine

Lancer une migration
--------------------

- Exporter la bd wordpress de travail dans le répertoire de migration. Par exemple dans `wp_dev.sql`.
- Lancer l'export avec le fichier d'export spip et le fichier d'export de la bd wordpress de travail:

    java -jar migrate-tbz.jar tbz_spip.sql wp_dev.sql

- Patienter (ca prends un bon 5 min)
- Modifier la config wordpress avec les infos suivantes:
  - `DB_USER`: tbz
  - `DB_PASSWORD`: tbzpwd
  - `DB_NAME`: tbz_wp_migrated
  
Et voilà, tu peux aller admirer le résultat :).  

Explication des messages d'erreur:
----------------------------------

Tu vas voir, il y a plein de petits glitchs lors d'une migration. Voici les explications.


`21:09:28.043 [main] ERROR tbz.PostMigrator - Cannot treat \[((?:[^-]|-[^>])*)\-\>http://www.tribalzine.com/\?([^\]]+)\] for post 2010 (SO)`
Pas réussi à analyser l'article (ca fait un stack overflow, ou SO). Il faut vérifier cet article à la main.

`java.lang.Exception: Cannot find internal link for Danny-Macaskill-a-la-Une-de-Trial in [la Une de Trial Mag' ->http://www.tribalzine.com/?Danny-Macaskill-a-la-Une-de-Trial] for post 4372`
Réussi à analyser l'article, mais pas trouvé le lien dans la base spip. Probablement un article qui fut supprimé par la suite. Il faut corriger cet article à la main, soit en retrouvant l'article, soit en supprimant le lien.
Note: peut se fixer d'avance directement dans SPIP (le numéro d'article est le bon jusqu'à 6000, ensuite c'est les brèves, il faut enlever 6000 au numéro du post pour trouver l'id de la brève)

`21:09:25.309 [main] WARN  tbz.PostMigrator - Cannot found attachment for spip tag <img10473|center> in post 839. Image will not be displayed.`
Ah, ben on a pas trouvé le document spip en question. Il a probablement été supprimé à un moment. Il faut corriger cet article à la main (probablement en supprimant l'image).
Note: peut se fixer d'avance directement dans SPIP (le numéro d'article est le bon jusqu'à 6000, ensuite c'est les brèves, il faut enlever 6000 au numéro du post pour trouver l'id de la brève)

Trucs à faire gaffe par la suite
================================

- attention au fil RSS, qu'il ne renvoit pas tous les articles depuis le début!!!
- avoir une belle page pour les erreurs 404, car les anciennes URLs Spip ne fonctionneront plus, il faudra peut être expliqué que les URLs de la v2 ont été changées.

Code source
===========

Si tu es curieux, le code sources est sur GitHub: [https://github.com/jletroui/tbz-migration](https://github.com/jletroui/tbz-migration).