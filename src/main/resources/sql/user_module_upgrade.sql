-- Script de mise à jour de la table user pour les nouvelles fonctionnalités (Reset Password, OAuth, Verification)
-- À exécuter manuellement dans votre client MySQL si vous voyez l'erreur 'Unknown column'

ALTER TABLE `user` 
ADD COLUMN IF NOT EXISTS reset_token VARCHAR(255) NULL,
ADD COLUMN IF NOT EXISTS reset_token_expiry DATETIME NULL,
ADD COLUMN IF NOT EXISTS login_provider VARCHAR(20) DEFAULT 'local',
ADD COLUMN IF NOT EXISTS last_login_at DATETIME NULL,
ADD COLUMN IF NOT EXISTS verification_token VARCHAR(255) NULL;

-- Vérification des types
-- Si la table existe déjà sans ces colonnes, les commandes ci-dessus les ajoutent.
-- Si elles existent déjà, SQL ignorera l'erreur grâce à 'IF NOT EXISTS' (selon votre version de MariaDB/MySQL).
-- Si 'IF NOT EXISTS' n'est pas supporté, exécutez simplement les lignes ADD COLUMN sans ce mot-clé.
