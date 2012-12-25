// Licensed to the Apache Software Foundation (ASF) under one
// or more contributor license agreements.  See the NOTICE file
// distributed with this work for additional information
// regarding copyright ownership.  The ASF licenses this file
// to you under the Apache License, Version 2.0 (the
// "License"); you may not use this file except in compliance
// with the License.  You may obtain a copy of the License at
//
//   http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing,
// software distributed under the License is distributed on an
// "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
// KIND, either express or implied.  See the License for the
// specific language governing permissions and limitations
// under the License.
package com.cloud.agent.manager;

import java.util.HashMap;
import java.util.Map;

import javax.ejb.Local;
import javax.naming.ConfigurationException;

import com.cloud.agent.api.*;
import com.cloud.agent.api.storage.*;
import org.apache.log4j.Logger;

import com.cloud.agent.api.check.CheckSshCommand;
import com.cloud.agent.api.proxy.CheckConsoleProxyLoadCommand;
import com.cloud.agent.api.proxy.WatchConsoleProxyLoadCommand;
import com.cloud.agent.api.routing.DhcpEntryCommand;
import com.cloud.agent.api.routing.IpAssocCommand;
import com.cloud.agent.api.routing.LoadBalancerConfigCommand;
import com.cloud.agent.api.routing.SavePasswordCommand;
import com.cloud.agent.api.routing.SetFirewallRulesCommand;
import com.cloud.agent.api.routing.SetPortForwardingRulesCommand;
import com.cloud.agent.api.routing.SetStaticNatRulesCommand;
import com.cloud.agent.api.routing.VmDataCommand;
import com.cloud.simulator.MockConfigurationVO;
import com.cloud.simulator.MockHost;
import com.cloud.simulator.MockVMVO;
import com.cloud.simulator.dao.MockConfigurationDao;
import com.cloud.simulator.dao.MockHostDao;
import com.cloud.utils.Pair;
import com.cloud.utils.component.Inject;
import com.cloud.utils.db.ConnectionConcierge;
import com.cloud.utils.db.DB;
import com.cloud.utils.db.Transaction;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.vm.VirtualMachine.State;

@Local(value = { SimulatorManager.class })
public class SimulatorManagerImpl implements SimulatorManager {
    private static final Logger s_logger = Logger.getLogger(SimulatorManagerImpl.class);
    @Inject
    MockVmManager _mockVmMgr = null;
    @Inject
    MockStorageManager _mockStorageMgr = null;
    @Inject
    MockAgentManager _mockAgentMgr = null;
    @Inject
    MockConfigurationDao _mockConfigDao = null;
    @Inject
    MockHostDao _mockHost = null;
    private ConnectionConcierge _concierge;
    @Override
    public boolean configure(String name, Map<String, Object> params) throws ConfigurationException {
    /*
        try {
            Connection conn = Transaction.getStandaloneSimulatorConnection();
            conn.setAutoCommit(true);
            _concierge = new ConnectionConcierge("SimulatorConnection", conn, true);
        } catch (SQLException e) {
            throw new CloudRuntimeException("Unable to get a db connection to simulator", e);
        }
	*/
        return true;
    }

    @Override
    public boolean start() {
        return true;
    }

    @Override
    public boolean stop() {
        return true;
    }

    @Override
    public String getName() {
        return this.getClass().getSimpleName();
    }

    @Override
    public MockVmManager getVmMgr() {
        return _mockVmMgr;
    }

    @Override
    public MockStorageManager getStorageMgr() {
        return _mockStorageMgr;
    }

    @Override
    public MockAgentManager getAgentMgr() {
        return _mockAgentMgr;
    }

    @DB
    @Override
    public Answer simulate(Command cmd, String hostGuid) {
        Transaction txn = Transaction.open(Transaction.SIMULATOR_DB);
 //       txn.transitToUserManagedConnection(_concierge.conn());

        try {
            MockHost host = _mockHost.findByGuid(hostGuid);
            String cmdName = cmd.toString();
            int index = cmdName.lastIndexOf(".");
            if (index != -1) {
		cmdName = cmdName.substring(index + 1);
            }
            MockConfigurationVO config = _mockConfigDao.findByNameBottomUP(host.getDataCenterId(), host.getPodId(), host.getClusterId(), host.getId(), cmdName);

            SimulatorInfo info = new SimulatorInfo();
            info.setHostUuid(hostGuid);

            if (config != null) {
                Map<String, String> configParameters = config.getParameters();
                for (Map.Entry<String, String> entry : configParameters.entrySet()) {
			if (entry.getKey().equalsIgnoreCase("enabled")) {
				info.setEnabled(Boolean.parseBoolean(entry.getValue()));
			} else if (entry.getKey().equalsIgnoreCase("timeout")) {
				try {
					info.setTimeout(Integer.valueOf(entry.getValue()));
				} catch (NumberFormatException e) {
					s_logger.debug("invalid timeout parameter: " + e.toString());
				}
			} else if (entry.getKey().equalsIgnoreCase("wait")) {
				try {
					int wait = Integer.valueOf(entry.getValue());
					Thread.sleep(wait * 1000);
				} catch (NumberFormatException e) {
					s_logger.debug("invalid timeout parameter: " + e.toString());
				} catch (InterruptedException e) {
					s_logger.debug("thread is interrupted: " + e.toString());
				}
			}
                }
            }

            if (cmd instanceof GetHostStatsCommand) {
                return _mockAgentMgr.getHostStatistic((GetHostStatsCommand) cmd);
            } else if (cmd instanceof CheckHealthCommand) {
                return _mockAgentMgr.checkHealth((CheckHealthCommand) cmd);
            } else if (cmd instanceof PingTestCommand) {
                return _mockAgentMgr.pingTest((PingTestCommand) cmd);
            } else if (cmd instanceof PrepareForMigrationCommand) {
			return _mockVmMgr.prepareForMigrate((PrepareForMigrationCommand) cmd);
            } else if (cmd instanceof MigrateCommand) {
                return _mockVmMgr.Migrate((MigrateCommand) cmd, info);
            } else if (cmd instanceof StartCommand) {
                return _mockVmMgr.startVM((StartCommand) cmd, info);
            } else if (cmd instanceof CheckSshCommand) {
                return _mockVmMgr.checkSshCommand((CheckSshCommand) cmd);
            } else if (cmd instanceof CheckVirtualMachineCommand) {
			return _mockVmMgr.checkVmState((CheckVirtualMachineCommand) cmd);
            } else if (cmd instanceof SetStaticNatRulesCommand) {
                return _mockVmMgr.SetStaticNatRules((SetStaticNatRulesCommand) cmd);
            } else if (cmd instanceof SetFirewallRulesCommand) {
			return _mockVmMgr.SetFirewallRules((SetFirewallRulesCommand) cmd);
            } else if (cmd instanceof SetPortForwardingRulesCommand) {
                return _mockVmMgr.SetPortForwardingRules((SetPortForwardingRulesCommand) cmd);
            } else if (cmd instanceof NetworkUsageCommand) {
                return _mockVmMgr.getNetworkUsage((NetworkUsageCommand) cmd);
            } else if (cmd instanceof IpAssocCommand) {
                return _mockVmMgr.IpAssoc((IpAssocCommand) cmd);
            } else if (cmd instanceof LoadBalancerConfigCommand) {
                return _mockVmMgr.LoadBalancerConfig((LoadBalancerConfigCommand) cmd);
            } else if (cmd instanceof DhcpEntryCommand) {
                return _mockVmMgr.AddDhcpEntry((DhcpEntryCommand) cmd);
            } else if (cmd instanceof VmDataCommand) {
                return _mockVmMgr.setVmData((VmDataCommand) cmd);
            } else if (cmd instanceof CleanupNetworkRulesCmd) {
                return _mockVmMgr.CleanupNetworkRules((CleanupNetworkRulesCmd) cmd, info);
            } else if (cmd instanceof CheckNetworkCommand) {
        		return _mockAgentMgr.checkNetworkCommand((CheckNetworkCommand) cmd);
            }else if (cmd instanceof StopCommand) {
                return _mockVmMgr.stopVM((StopCommand)cmd);
            } else if (cmd instanceof RebootCommand) {
                return _mockVmMgr.rebootVM((RebootCommand) cmd);
            } else if (cmd instanceof GetVncPortCommand) {
                return _mockVmMgr.getVncPort((GetVncPortCommand)cmd);
            } else if (cmd instanceof CheckConsoleProxyLoadCommand) {
                return _mockVmMgr.CheckConsoleProxyLoad((CheckConsoleProxyLoadCommand)cmd);
            } else if (cmd instanceof WatchConsoleProxyLoadCommand) {
                return _mockVmMgr.WatchConsoleProxyLoad((WatchConsoleProxyLoadCommand)cmd);
            } else if (cmd instanceof SecurityGroupRulesCmd) {
                return _mockVmMgr.AddSecurityGroupRules((SecurityGroupRulesCmd)cmd, info);
            } else if (cmd instanceof SavePasswordCommand) {
                return _mockVmMgr.SavePassword((SavePasswordCommand)cmd);
            } else if (cmd instanceof PrimaryStorageDownloadCommand) {
                return _mockStorageMgr.primaryStorageDownload((PrimaryStorageDownloadCommand)cmd);
            } else if (cmd instanceof CreateCommand) {
                return _mockStorageMgr.createVolume((CreateCommand)cmd);
            } else if (cmd instanceof AttachVolumeCommand) {
                return _mockStorageMgr.AttachVolume((AttachVolumeCommand)cmd);
            } else if (cmd instanceof AttachIsoCommand) {
                return _mockStorageMgr.AttachIso((AttachIsoCommand)cmd);
            } else if (cmd instanceof DeleteStoragePoolCommand) {
                return _mockStorageMgr.DeleteStoragePool((DeleteStoragePoolCommand)cmd);
            } else if (cmd instanceof ModifyStoragePoolCommand) {
                return _mockStorageMgr.ModifyStoragePool((ModifyStoragePoolCommand)cmd);
            } else if (cmd instanceof CreateStoragePoolCommand) {
                return _mockStorageMgr.CreateStoragePool((CreateStoragePoolCommand)cmd);
            } else if (cmd instanceof SecStorageSetupCommand) {
                return _mockStorageMgr.SecStorageSetup((SecStorageSetupCommand)cmd);
            } else if (cmd instanceof ListTemplateCommand) {
                return _mockStorageMgr.ListTemplates((ListTemplateCommand)cmd);
            } else if (cmd instanceof ListVolumeCommand) {
                return _mockStorageMgr.ListVolumes((ListVolumeCommand)cmd);
            } else if (cmd instanceof DestroyCommand) {
                return _mockStorageMgr.Destroy((DestroyCommand)cmd);
            } else if (cmd instanceof DownloadProgressCommand) {
                return _mockStorageMgr.DownloadProcess((DownloadProgressCommand)cmd);
            } else if (cmd instanceof DownloadCommand) {
                return _mockStorageMgr.Download((DownloadCommand)cmd);
            } else if (cmd instanceof GetStorageStatsCommand) {
                return _mockStorageMgr.GetStorageStats((GetStorageStatsCommand)cmd);
            } else if (cmd instanceof ManageSnapshotCommand) {
                return _mockStorageMgr.ManageSnapshot((ManageSnapshotCommand)cmd);
            } else if (cmd instanceof BackupSnapshotCommand) {
                return _mockStorageMgr.BackupSnapshot((BackupSnapshotCommand)cmd, info);
            } else if (cmd instanceof DeleteSnapshotBackupCommand) {
                return _mockStorageMgr.DeleteSnapshotBackup((DeleteSnapshotBackupCommand)cmd);
            } else if (cmd instanceof CreateVolumeFromSnapshotCommand) {
                return _mockStorageMgr.CreateVolumeFromSnapshot((CreateVolumeFromSnapshotCommand)cmd);
            } else if (cmd instanceof DeleteTemplateCommand) {
                return _mockStorageMgr.DeleteTemplate((DeleteTemplateCommand)cmd);
            } else if (cmd instanceof SecStorageVMSetupCommand) {
                return _mockStorageMgr.SecStorageVMSetup((SecStorageVMSetupCommand)cmd);
            } else if (cmd instanceof CreatePrivateTemplateFromSnapshotCommand) {
                return _mockStorageMgr.CreatePrivateTemplateFromSnapshot((CreatePrivateTemplateFromSnapshotCommand)cmd);
            } else if (cmd instanceof ComputeChecksumCommand) {
                return _mockStorageMgr.ComputeChecksum((ComputeChecksumCommand)cmd);
            } else if (cmd instanceof CreatePrivateTemplateFromVolumeCommand) {
                return _mockStorageMgr.CreatePrivateTemplateFromVolume((CreatePrivateTemplateFromVolumeCommand)cmd);
            } else if (cmd instanceof MaintainCommand) {
                return _mockAgentMgr.maintain((MaintainCommand)cmd);
            } else if (cmd instanceof GetVmStatsCommand) {
                return _mockVmMgr.getVmStats((GetVmStatsCommand)cmd);
            } else if (cmd instanceof CheckRouterCommand) {
                return _mockVmMgr.checkRouter((CheckRouterCommand) cmd);
            } else if (cmd instanceof BumpUpPriorityCommand) {
                return _mockVmMgr.bumpPriority((BumpUpPriorityCommand) cmd);
            } else if (cmd instanceof GetDomRVersionCmd) {
		        return _mockVmMgr.getDomRVersion((GetDomRVersionCmd) cmd);
            } else if (cmd instanceof ClusterSyncCommand) {
        		return new Answer(cmd);
            } else if (cmd instanceof CopyVolumeCommand) {
	        	return _mockStorageMgr.CopyVolume((CopyVolumeCommand) cmd);
            } else {
                return Answer.createUnsupportedCommandAnswer(cmd);
            }
        } catch(Exception e) {
            s_logger.error("Failed execute cmd: " + e.toString());
            txn.rollback();
            return new Answer(cmd, false, e.toString());
        } finally {
            txn.close();
            txn = Transaction.open(Transaction.CLOUD_DB);
            txn.close();
        }
    }

    @Override
    public StoragePoolInfo getLocalStorage(String hostGuid) {
        return _mockStorageMgr.getLocalStorage(hostGuid);
    }

    @Override
    public Map<String, State> getVmStates(String hostGuid) {
	return _mockVmMgr.getVmStates(hostGuid);
    }

    @Override
    public Map<String, MockVMVO> getVms(String hostGuid) {
	return _mockVmMgr.getVms(hostGuid);
    }

    @Override
    public HashMap<String, Pair<Long, Long>> syncNetworkGroups(String hostGuid) {
	SimulatorInfo info = new SimulatorInfo();
	info.setHostUuid(hostGuid);
	return _mockVmMgr.syncNetworkGroups(info);
    }

    @Override
	public boolean configureSimulator(Long zoneId, Long podId, Long clusterId, Long hostId, String command,
			String values) {
		Transaction txn = Transaction.open(Transaction.SIMULATOR_DB);
		try {
			txn.start();
			MockConfigurationVO config = _mockConfigDao.findByCommand(zoneId, podId, clusterId, hostId, command);
			if (config == null) {
				config = new MockConfigurationVO();
				config.setClusterId(clusterId);
				config.setDataCenterId(zoneId);
				config.setPodId(podId);
				config.setHostId(hostId);
				config.setName(command);
				config.setValues(values);
				_mockConfigDao.persist(config);
				txn.commit();
			} else {
				config.setValues(values);
				_mockConfigDao.update(config.getId(), config);
				txn.commit();
			}
		} catch (Exception ex) {
			txn.rollback();
			throw new CloudRuntimeException("Unable to configure simulator because of " + ex.getMessage(), ex);
		} finally {
			txn.close();
		}
		return true;
	}
}
