INSERT INTO role_path (RolePath_id, Parent_RolePath_id, Root, Path) VALUES (10, NULL, 'role', 'weighbridge-operator');
INSERT INTO role_path (RolePath_id, Parent_RolePath_id, Root, Path) VALUES (20, NULL, 'role', 'binmap-manager');
INSERT INTO role_path (RolePath_id, Parent_RolePath_id, Root, Path) VALUES (30, NULL, 'role', 'report-designer');

INSERT INTO item_path (ItemPath_id, Root, Uuid, Ior, Path) VALUES (1, 'iRoot1', 'iUuid1', 'iIor1', 'iPath1');
INSERT INTO item_path (ItemPath_id, Root, Uuid, Ior, Path) VALUES (2, 'iRoot2', 'iUuid2', 'iIor2', 'iPath2');
INSERT INTO item_path (ItemPath_id, Root, Uuid, Ior, Path) VALUES (3, 'iRoot3', 'iUuid3', 'iIor3', 'iPath3');

INSERT INTO domain_path (DomainPath_id, Parent_DomainPath_id, Target_ItemPath_id, Root, Path) VALUES (1, NULL, 2, 'dRoot1', 'dPath1');
INSERT INTO domain_path (DomainPath_id, Parent_DomainPath_id, Target_ItemPath_id, Root, Path) VALUES (2, NULL, 3, 'dRoot2', 'dPath2');
INSERT INTO domain_path (DomainPath_id, Parent_DomainPath_id, Target_ItemPath_id, Root, Path) VALUES (3, NULL, 1, 'dRoot3', 'dPath3');

INSERT INTO agent_path (AgentPath_id, Agent_Name, Agent_Password, Root, Agent_Uuid, Ior, Path) VALUES (1, 'Zoli', 'pwd1', 'agentroot1', 'auuid1', 'aior1', 'apath1');
INSERT INTO agent_path (AgentPath_id, Agent_Name, Agent_Password, Root, Agent_Uuid, Ior, Path) VALUES (2, 'Robi', 'pwd2', 'agentroot2', 'auuid2', 'aior2', 'apath2');

INSERT INTO agent_role (AgentPath_id, RolePath_id) VALUES (1, 10);
INSERT INTO agent_role (AgentPath_id, RolePath_id) VALUES (2, 10);
INSERT INTO agent_role (AgentPath_id, RolePath_id) VALUES (1, 20);
INSERT INTO agent_role (AgentPath_id, RolePath_id) VALUES (2, 30);
