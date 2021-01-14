/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package ru.alex.testwork.service.impl;

import org.springframework.stereotype.Service;
import ru.alex.testwork.domain.UserD;
import ru.alex.testwork.service.UserService;

import java.util.Collection;
import java.util.Map;
import java.util.TreeMap;


@Service("userService")
public class UserServiceImpl implements UserService {

    private final Map<Integer, UserD> users = new TreeMap<>();

    public UserServiceImpl() {
        users.put(1, new UserD(1, "Юрий Никулин"));
        users.put(2, new UserD(2, "Miles Davis"));
        users.put(3, new UserD(3, "Sonny Rollins"));
    }

    @Override
    public UserD findUser(Integer id) {
        return users.get(id);
    }

    @Override
    public Collection<UserD> findUsers() {
        return users.values();
    }

    @Override
    public void updateUser(UserD user) {
        users.put(user.getId(), user);
    }

}