CREATE TABLE role_path (
    RolePath_id        int(11)       NOT NULL,
    Parent_RolePath_id int(11)               ,
    Root               varchar(20)   NOT NULL,
    Path               varchar(4000) NOT NULL,
    PRIMARY KEY (RolePath_id),
    FOREIGN KEY (Parent_RolePath_id) REFERENCES role_path(RolePath_id));

CREATE TABLE item_path (
    ItemPath_id int(11)       NOT NULL,
    Root        varchar(20)   NOT NULL,
    Uuid        varchar(256)  NOT NULL,
    Ior         varchar(4000) NOT NULL,
    Path        varchar(4000) NOT NULL,
    PRIMARY KEY (ItemPath_id));
                  
CREATE TABLE domain_path (
    DomainPath_id        int(11)       NOT NULL,
    Parent_DomainPath_id int(11)               ,
    Target_ItemPath_id   int(11)       NOT NULL,
    Root                 varchar(20)   NOT NULL,
    Path                 varchar(4000) NOT NULL,
    PRIMARY KEY (DomainPath_id),
    FOREIGN KEY (Target_ItemPath_id)   REFERENCES item_path(ItemPath_id),
    FOREIGN KEY (Parent_DomainPath_id) REFERENCES domain_path(DomainPath_id));
 
CREATE TABLE agent_path (
    AgentPath_id   int(11)       NOT NULL,
    Agent_Name     varchar(50)   NOT NULL,
    Agent_Password varchar(50)   NOT NULL,
    Root           varchar(20)   NOT NULL,
    Agent_Uuid     varchar(256)  NOT NULL,
    Ior            varchar(4000) NOT NULL,
    Path           varchar(4000) NOT NULL,
    PRIMARY KEY (AgentPath_id));

CREATE TABLE agent_role (
    AgentPath_id int(11) NOT NULL,
    RolePath_id  int(11) NOT NULL,
    CONSTRAINT agent_role_keys PRIMARY KEY (AgentPath_id, RolePath_id),
    FOREIGN KEY (AgentPath_id) REFERENCES agent_path(AgentPath_id),
    FOREIGN KEY (RolePath_id)  REFERENCES role_path(RolePath_id));
