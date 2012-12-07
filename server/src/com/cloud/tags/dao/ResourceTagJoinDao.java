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
package com.cloud.tags.dao;

import java.util.List;

import com.cloud.api.response.ResourceTagResponse;
import com.cloud.api.view.vo.ResourceTagJoinVO;
import com.cloud.server.ResourceTag;
import com.cloud.user.Account;
import com.cloud.utils.db.GenericDao;

public interface ResourceTagJoinDao extends GenericDao<ResourceTagJoinVO, Long> {

    ResourceTagResponse newResourceTagResponse(ResourceTagJoinVO uvo, boolean keyValueOnly );

    ResourceTagJoinVO newResourceTagView(ResourceTag vr);

    List<ResourceTagJoinVO> searchByIds(Long... ids);
}
