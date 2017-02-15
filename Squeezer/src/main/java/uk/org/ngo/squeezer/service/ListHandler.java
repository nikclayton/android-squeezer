/*
 * Copyright (c) 2012 Kurt Aaholst <kaaholst@gmail.com>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package uk.org.ngo.squeezer.service;

import java.util.List;
import java.util.Map;

import uk.org.ngo.squeezer.framework.Item;

/**
 * Implement this and give it to {@link CliClient#parseSqueezerList(CliClient.ExtendedQueryFormatCmd, List)} for each
 * extended query format command you wish to support. </p>
 *
 * @author Kurt Aaholst
 */
interface ListHandler<T extends Item> {
    /**
     * @return The type of item this handler can handle
     */
    Class<T> getDataType();

    /**
     * @return The list of items received so far
     */
    public List<T> getItems();

    /**
     * Prepare for parsing an extended query format response
     */
    void clear();

    /**
     * Called for each item received in the current reply. Just store this internally.
     *
     * @param record Item data from Squeezebox Server
     */
    void add(Map<String, String> record);
}
