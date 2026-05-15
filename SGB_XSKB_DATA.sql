-- "shujuzhilibu"."SGB_XSKB_SHENHE_0508" definition

-- Drop table

-- DROP TABLE "shujuzhilibu"."SGB_XSKB_SHENHE_0508";

CREATE TABLE "shujuzhilibu"."SGB_XSKB_SHENHE_0508" (
	"CRE_USER_ID" character varying(64 char) NULL,
	"HANDLE_USER_ID" character varying(64 char) NULL,
	"OP_USER_ID" character varying(64 char) NULL,
	"OP_USER_NAME" character varying(200 char) NULL,
	"CRE_USER_NAME" character varying(200 char) NULL,
	"HANDLE_STATUS" text NULL,
	"PROBLEM_ID" character varying(64 char) NULL,
	"HANDLE_ROLE_ID" character varying(64 char) NULL,
	"JHPT_DELETE" text NULL,
	"DATA_UPDATE_TIME" text NULL,
	"IS_SYNC_VISIT" text NULL,
	"HANDLE_PIC" text NULL,
	"NODE_NAME" character varying(200 char) NULL,
	"JHPT_UPDATE_TIME" text NULL,
	"HANDLE_ID" character varying(64 char) NULL,
	"HANDLE_CONTENT" text NULL,
	"OP_TIME" timestamp without time zone NULL,
	"DEPT_ID" character varying(64 char) NULL,
	"HANDLE_TYPE" text NULL,
	"CRE_TIME" timestamp without time zone NULL,
	"DSJZX_TASKID" character varying(10 char) NULL
);



-- "shujuzhilibu"."SGB_XSKB_SHENQING_0508" definition

-- Drop table

-- DROP TABLE "shujuzhilibu"."SGB_XSKB_SHENQING_0508";

CREATE TABLE "shujuzhilibu"."SGB_XSKB_SHENQING_0508" (
	"OP_USER_NAME" character varying(200 char) NULL,
	"JHPT_DELETE" text NULL,
	"PROBLEM_IMG" text NULL,
	"JHPT_UPDATE_TIME" text NULL,
	"OP_TIME" timestamp without time zone NULL,
	"PERSON_PHONE" character varying(200 char) NULL,
	"PROBLEM_TIME" timestamp without time zone NULL,
	"PROBLEM_STATUS" text NULL,
	"VISIT_COUNT" integer NULL,
	"PROBLEM_ID" character varying(64 char) NULL,
	"JW_ID" character varying(64 char) NULL,
	"DATA_UPDATE_TIME" text NULL,
	"PROBLEM_DESCRIPTION" text NULL,
	"PROBLEM_ADDRESS" character varying(200 char) NULL,
	"PROBLEM_TAGS" text NULL,
	"PERSON_NAME" character varying(200 char) NULL,
	"COMPLETED_TIME" timestamp without time zone NULL,
	"OP_USER_ID" character varying(64 char) NULL,
	"PROBLEM_TYPE" text NULL,
	"LAST_VISIT_TIME" timestamp without time zone NULL,
	"PERSON_ID" character varying(64 char) NULL,
	"DSJZX_TASKID" character varying(10 char) NULL
);


-- "shujuzhilibu"."SGB_XSKB_ZOUFANG_0508" definition

-- Drop table

-- DROP TABLE "shujuzhilibu"."SGB_XSKB_ZOUFANG_0508";

CREATE TABLE "shujuzhilibu"."SGB_XSKB_ZOUFANG_0508" (
	"CRE_TIME" timestamp without time zone NULL,
	"DATA_UPDATE_TIME" text NULL,
	"JHPT_DELETE" text NULL,
	"CRE_USER_ID" character varying(64 char) NULL,
	"DEPT_ID" character varying(64 char) NULL,
	"VISIT_SATISFACTION" text NULL,
	"PROBLEM_ID" character varying(64 char) NULL,
	"VISIT_ID" character varying(64 char) NULL,
	"JHPT_UPDATE_TIME" text NULL,
	"VISIT_REMARK" text NULL,
	"CRE_USER_NAME" character varying(200 char) NULL,
	"DSJZX_TASKID" character varying(10 char) NULL
);