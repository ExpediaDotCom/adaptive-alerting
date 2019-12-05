/*
 * Copyright 2018-2019 Expedia Group, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.expedia.adaptivealerting.anomdetect.forecast.point.algo.sma;

import com.opencsv.bean.CsvBindByName;

import lombok.Data;
import lombok.ToString;

@Data
@ToString
public class SmaPointForecasterTestRow {

    @CsvBindByName
    private int rownum;

    @CsvBindByName
    private double observed;

    @CsvBindByName
    private double sma1;

    @CsvBindByName
    private double sma3;

    @CsvBindByName
    private double sma9;

    @CsvBindByName
    private double sma21;
}
