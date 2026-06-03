CREATE DATABASE  IF NOT EXISTS `fraud_detection` /*!40100 DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci */ /*!80016 DEFAULT ENCRYPTION='N' */;
USE `fraud_detection`;
-- MySQL dump 10.13  Distrib 8.0.45, for Win64 (x86_64)
--
-- Host: localhost    Database: fraud_detection
-- ------------------------------------------------------
-- Server version	8.0.45

/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!50503 SET NAMES utf8 */;
/*!40014 SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;

--
-- Table structure for table `claim_investigations`
--

DROP TABLE IF EXISTS claim_investigations;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE claim_investigations (
  INVESTIGATION_ID bigint NOT NULL AUTO_INCREMENT,
  RAW_CLAIM_ID bigint NOT NULL,
  INVESTIGATOR_ID bigint NOT NULL,
  `ACTION` varchar(30) NOT NULL,
  NOTE text,
  CREATED_AT timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (INVESTIGATION_ID),
  KEY FK_INV_CLAIM (RAW_CLAIM_ID),
  KEY FK_INV_USER (INVESTIGATOR_ID),
  CONSTRAINT FK_INV_CLAIM FOREIGN KEY (RAW_CLAIM_ID) REFERENCES raw_claims (RAW_CLAIM_ID),
  CONSTRAINT FK_INV_USER FOREIGN KEY (INVESTIGATOR_ID) REFERENCES users (USER_ID)
) ENGINE=InnoDB AUTO_INCREMENT=3 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `claim_investigations`
--

INSERT INTO claim_investigations VALUES (1,15,3,'NOTE','aaaa','2026-06-03 19:50:14');
INSERT INTO claim_investigations VALUES (2,15,3,'APPROVE','','2026-06-03 19:50:33');

--
-- Table structure for table `claim_predictions`
--

DROP TABLE IF EXISTS claim_predictions;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE claim_predictions (
  PREDICTION_ID bigint NOT NULL AUTO_INCREMENT,
  RAW_CLAIM_ID bigint NOT NULL,
  MODEL_NAME varchar(100) DEFAULT NULL,
  MODEL_VERSION varchar(50) DEFAULT NULL,
  PREDICTED_LABEL varchar(50) DEFAULT NULL,
  ANOMALY_SCORE decimal(10,4) DEFAULT NULL,
  RISK_PERCENTAGE decimal(5,2) DEFAULT NULL,
  SHOULD_ALERT tinyint(1) DEFAULT NULL,
  SHAP_SUMMARY text,
  SHAP_CONFIDENCE int DEFAULT NULL,
  SHAP_METHOD varchar(20) DEFAULT NULL,
  PREDICTED_AT timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (PREDICTION_ID),
  KEY FK_PREDICTION_CLAIM (RAW_CLAIM_ID),
  CONSTRAINT FK_PREDICTION_CLAIM FOREIGN KEY (RAW_CLAIM_ID) REFERENCES raw_claims (RAW_CLAIM_ID) ON DELETE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=15 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `claim_predictions`
--

INSERT INTO claim_predictions VALUES (8,13,'IsolationForest','1.0.0','NORMAL',0.0705,33.06,0,'Claim falls within normal distribution parameters. Not flagged.',0,NULL,'2026-05-30 18:30:33');
INSERT INTO claim_predictions VALUES (9,15,'IsolationForest','1.0.0','ANOMALY',-0.0090,52.26,0,'CLAIM FLAGGED (52.3% Risk). Key driving factors: ICD9_PRCDR_CD_1 is increases anomaly risk (value: 20.0, impact: 1.618) | CLM_DRG_CD is increases anomaly risk (value: 72995.0, impact: 1.047) | ADMTNG_ICD9_DGNS_CD is increases anomaly risk (value: 49997.0, impact: 0.839)',1,'SHAP','2026-06-03 00:39:45');
INSERT INTO claim_predictions VALUES (10,14,'IsolationForest','1.0.0','NORMAL',0.0118,47.05,0,'Claim falls within normal distribution parameters. Not flagged.',0,NULL,'2026-06-03 00:49:33');
INSERT INTO claim_predictions VALUES (11,15,'IsolationForest','1.0.0','ANOMALY',-0.0090,52.26,0,'CLAIM FLAGGED (52.3% Risk). Key driving factors: ICD9_PRCDR_CD_1 is increases anomaly risk (value: 20.0, impact: 1.618) | CLM_DRG_CD is increases anomaly risk (value: 72995.0, impact: 1.047) | ADMTNG_ICD9_DGNS_CD is increases anomaly risk (value: 49997.0, impact: 0.839)',1,'SHAP','2026-06-03 00:52:55');
INSERT INTO claim_predictions VALUES (12,15,'IsolationForest','1.0.0','ANOMALY',-0.0090,52.26,0,'CLAIM FLAGGED (52.3% Risk). Key driving factors: ICD9_PRCDR_CD_1 is increases anomaly risk (value: 20.0, impact: 1.618) | CLM_DRG_CD is increases anomaly risk (value: 72995.0, impact: 1.047) | ADMTNG_ICD9_DGNS_CD is increases anomaly risk (value: 49997.0, impact: 0.839)',1,'SHAP','2026-06-03 00:52:57');
INSERT INTO claim_predictions VALUES (13,16,'IsolationForest','1.0.0','NORMAL',0.0685,33.51,0,'Claim falls within normal distribution parameters. Not flagged.',0,NULL,'2026-06-03 01:28:12');
INSERT INTO claim_predictions VALUES (14,17,'IsolationForest','1.0.0','NORMAL',0.0239,44.05,0,'Claim falls within normal distribution parameters. Not flagged.',0,NULL,'2026-06-03 01:53:07');

--
-- Table structure for table `claim_shap_factors`
--

DROP TABLE IF EXISTS claim_shap_factors;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE claim_shap_factors (
  SHAP_ID bigint NOT NULL AUTO_INCREMENT,
  PREDICTION_ID bigint NOT NULL,
  FEATURE_NAME varchar(100) DEFAULT NULL,
  FEATURE_VALUE varchar(100) DEFAULT NULL,
  SHAP_IMPACT decimal(10,4) DEFAULT NULL,
  DIRECTION varchar(50) DEFAULT NULL,
  PRIMARY KEY (SHAP_ID),
  KEY FK_SHAP_PREDICTION (PREDICTION_ID),
  CONSTRAINT FK_SHAP_PREDICTION FOREIGN KEY (PREDICTION_ID) REFERENCES claim_predictions (PREDICTION_ID) ON DELETE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=22 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `claim_shap_factors`
--

INSERT INTO claim_shap_factors VALUES (13,9,'ICD9_PRCDR_CD_1','413',1.6180,'increases anomaly risk');
INSERT INTO claim_shap_factors VALUES (14,9,'CLM_DRG_CD','231',1.0470,'increases anomaly risk');
INSERT INTO claim_shap_factors VALUES (15,9,'ADMTNG_ICD9_DGNS_CD','11123',0.8390,'increases anomaly risk');
INSERT INTO claim_shap_factors VALUES (16,11,'ICD9_PRCDR_CD_1','413',1.6180,'increases anomaly risk');
INSERT INTO claim_shap_factors VALUES (17,11,'CLM_DRG_CD','231',1.0470,'increases anomaly risk');
INSERT INTO claim_shap_factors VALUES (18,11,'ADMTNG_ICD9_DGNS_CD','11123',0.8390,'increases anomaly risk');
INSERT INTO claim_shap_factors VALUES (19,12,'ICD9_PRCDR_CD_1','413',1.6180,'increases anomaly risk');
INSERT INTO claim_shap_factors VALUES (20,12,'CLM_DRG_CD','231',1.0470,'increases anomaly risk');
INSERT INTO claim_shap_factors VALUES (21,12,'ADMTNG_ICD9_DGNS_CD','11123',0.8390,'increases anomaly risk');

--
-- Table structure for table `raw_claims`
--

DROP TABLE IF EXISTS raw_claims;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE raw_claims (
  RAW_CLAIM_ID bigint NOT NULL AUTO_INCREMENT,
  DESYNPUF_ID varchar(50) DEFAULT NULL,
  CLM_ID varchar(50) DEFAULT NULL,
  SEGMENT varchar(10) DEFAULT NULL,
  CLM_FROM_DT date DEFAULT NULL,
  CLM_THRU_DT date DEFAULT NULL,
  PRVDR_NUM varchar(20) DEFAULT NULL,
  CLM_PMT_AMT decimal(12,2) DEFAULT NULL,
  NCH_PRMRY_PYR_CLM_PD_AMT decimal(12,2) DEFAULT NULL,
  AT_PHYSN_NPI varchar(20) DEFAULT NULL,
  OP_PHYSN_NPI varchar(20) DEFAULT NULL,
  OT_PHYSN_NPI varchar(20) DEFAULT NULL,
  CLM_ADMSN_DT date DEFAULT NULL,
  ADMTNG_ICD9_DGNS_CD varchar(20) DEFAULT NULL,
  CLM_PASS_THRU_PER_DIEM_AMT decimal(12,2) DEFAULT NULL,
  NCH_BENE_IP_DDCTBL_AMT decimal(12,2) DEFAULT NULL,
  NCH_BENE_PTA_COINSRNC_LBLTY_AM decimal(12,2) DEFAULT NULL,
  NCH_BENE_BLOOD_DDCTBL_LBLTY_AM decimal(12,2) DEFAULT NULL,
  CLM_UTLZTN_DAY_CNT int DEFAULT NULL,
  NCH_BENE_DSCHRG_DT date DEFAULT NULL,
  CLM_DRG_CD varchar(20) DEFAULT NULL,
  CLAIM_HANDLER_ID bigint DEFAULT NULL,
  INVESTIGATOR_ID bigint DEFAULT NULL,
  CLAIM_STATUS varchar(30) DEFAULT 'PENDING',
  VERSION bigint DEFAULT '0',
  PRIMARY KEY (RAW_CLAIM_ID),
  UNIQUE KEY CLM_ID (CLM_ID),
  KEY FK_CLAIM_HANDLER (CLAIM_HANDLER_ID),
  KEY FK_INVESTIGATOR (INVESTIGATOR_ID),
  CONSTRAINT FK_CLAIM_HANDLER FOREIGN KEY (CLAIM_HANDLER_ID) REFERENCES users (USER_ID),
  CONSTRAINT FK_INVESTIGATOR FOREIGN KEY (INVESTIGATOR_ID) REFERENCES users (USER_ID)
) ENGINE=InnoDB AUTO_INCREMENT=18 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `raw_claims`
--

INSERT INTO raw_claims VALUES (13,'13412341','1341424323','1','2026-05-11','2026-05-15','1002CR',25000.00,250.00,'1234567890','9876543210','5555555555','2026-05-01','78650',432.00,220.00,123.00,123.00,8,'2026-05-09','470',2,3,'PENDING',2);
INSERT INTO raw_claims VALUES (14,'sda121112','01212121','2','2026-05-13','2026-06-03','99000',123444.00,123.00,'123','342','233','2026-05-20','12314',41.00,444.00,1231.00,111.00,3,'2026-05-28','231',2,3,'UNDER_REVIEW',2);
INSERT INTO raw_claims VALUES (15,'anbn2121','1111111','1','2026-04-27','2026-06-03','2314',25666.00,124.00,'111','1113','4141','2026-05-21','11123',1222.00,1555.00,121.00,245.00,12,'2026-06-02','231',2,3,'APPROVED',3);
INSERT INTO raw_claims VALUES (16,'adfaf','1212','werwr','2026-05-04','2026-06-03','3342',12233.00,122.98,'244','12314','5541','2026-05-20','141241',441.00,12.00,441.00,342.00,3,'2026-05-30','123',2,3,'UNDER_REVIEW',2);
INSERT INTO raw_claims VALUES (17,NULL,'34234',NULL,'0012-03-21','0123-03-12','123132',2342.00,1313.00,'13123','1231','1321',NULL,'13123',NULL,NULL,NULL,NULL,1,NULL,'231231',2,5,'UNDER_REVIEW',2);

--
-- Table structure for table `raw_diagnoses`
--

DROP TABLE IF EXISTS raw_diagnoses;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE raw_diagnoses (
  DIAGNOSIS_ID bigint NOT NULL AUTO_INCREMENT,
  RAW_CLAIM_ID bigint NOT NULL,
  ICD9_DGNS_CD varchar(20) DEFAULT NULL,
  PRIMARY KEY (DIAGNOSIS_ID),
  KEY FK_DIAGNOSIS_CLAIM (RAW_CLAIM_ID),
  CONSTRAINT FK_DIAGNOSIS_CLAIM FOREIGN KEY (RAW_CLAIM_ID) REFERENCES raw_claims (RAW_CLAIM_ID) ON DELETE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=17 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `raw_diagnoses`
--

INSERT INTO raw_diagnoses VALUES (14,13,'78650');
INSERT INTO raw_diagnoses VALUES (15,14,'12314');
INSERT INTO raw_diagnoses VALUES (16,15,'22221');

--
-- Table structure for table `raw_hcpcs`
--

DROP TABLE IF EXISTS raw_hcpcs;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE raw_hcpcs (
  HCPCS_ID bigint NOT NULL AUTO_INCREMENT,
  RAW_CLAIM_ID bigint NOT NULL,
  HCPCS_CD varchar(20) DEFAULT NULL,
  PRIMARY KEY (HCPCS_ID),
  KEY FK_HCPCS_CLAIM (RAW_CLAIM_ID),
  CONSTRAINT FK_HCPCS_CLAIM FOREIGN KEY (RAW_CLAIM_ID) REFERENCES raw_claims (RAW_CLAIM_ID) ON DELETE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=11 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `raw_hcpcs`
--

INSERT INTO raw_hcpcs VALUES (9,13,'99233');
INSERT INTO raw_hcpcs VALUES (10,14,'122');

--
-- Table structure for table `raw_procedures`
--

DROP TABLE IF EXISTS raw_procedures;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE raw_procedures (
  PROCEDURE_ID bigint NOT NULL AUTO_INCREMENT,
  RAW_CLAIM_ID bigint NOT NULL,
  ICD9_PRCDR_CD varchar(20) DEFAULT NULL,
  PRIMARY KEY (PROCEDURE_ID),
  KEY FK_PROCEDURE_CLAIM (RAW_CLAIM_ID),
  CONSTRAINT FK_PROCEDURE_CLAIM FOREIGN KEY (RAW_CLAIM_ID) REFERENCES raw_claims (RAW_CLAIM_ID) ON DELETE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=14 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `raw_procedures`
--

INSERT INTO raw_procedures VALUES (9,13,'3991');
INSERT INTO raw_procedures VALUES (10,14,'242123');
INSERT INTO raw_procedures VALUES (11,15,'413');
INSERT INTO raw_procedures VALUES (12,16,'41241');
INSERT INTO raw_procedures VALUES (13,17,'242');

--
-- Table structure for table `users`
--

DROP TABLE IF EXISTS users;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE users (
  USER_ID bigint NOT NULL AUTO_INCREMENT,
  FULL_NAME varchar(255) NOT NULL,
  EMAIL varchar(255) NOT NULL,
  `PASSWORD` text NOT NULL,
  `ROLE` varchar(50) NOT NULL,
  DEPARTMENT varchar(100) DEFAULT NULL,
  PHONE_NUMBER varchar(20) DEFAULT NULL,
  `STATUS` varchar(20) DEFAULT 'ACTIVE',
  PRIMARY KEY (USER_ID),
  UNIQUE KEY EMAIL (EMAIL)
) ENGINE=InnoDB AUTO_INCREMENT=6 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `users`
--

INSERT INTO users VALUES (1,'Admin System','admin@fraud.com','$2a$10$N0h/twQZHH42SM.Ixfh5nuvtlGT2pn0fLi9itclRTb4eqmQ2QbCj2','ADMIN',NULL,NULL,'ACTIVE');
INSERT INTO users VALUES (2,'Nguyen Van A','staff@fraud.com','$2a$10$N0h/twQZHH42SM.Ixfh5nuvtlGT2pn0fLi9itclRTb4eqmQ2QbCj2','STAFF',NULL,NULL,'ACTIVE');
INSERT INTO users VALUES (3,'Tran Thi B','investigator@fraud.com','$2a$10$N0h/twQZHH42SM.Ixfh5nuvtlGT2pn0fLi9itclRTb4eqmQ2QbCj2','INVESTIGATOR',NULL,NULL,'ACTIVE');
INSERT INTO users VALUES (5,'Tran Van A','test@fraud.com','$2a$10$N0h/twQZHH42SM.Ixfh5nuvtlGT2pn0fLi9itclRTb4eqmQ2QbCj2','INVESTIGATOR',NULL,NULL,'ACTIVE');

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

-- Dump completed
