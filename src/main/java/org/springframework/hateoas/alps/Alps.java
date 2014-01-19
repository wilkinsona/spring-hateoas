/*
 * Copyright 2014 the original author or authors.
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
package org.springframework.hateoas.alps;

import java.util.List;

import lombok.Value;
import lombok.experimental.Builder;

import org.springframework.hateoas.alps.Descriptor.DescriptorBuilder;
import org.springframework.hateoas.alps.Doc.DocBuilder;
import org.springframework.hateoas.alps.Ext.ExtBuilder;

/**
 * @author Oliver Gierke
 */
@Value
@Builder(builderMethodName = "alps")
public class Alps {

	private String version = "1.0";
	private Doc doc;
	private List<Descriptor> descriptors;

	public static DescriptorBuilder descriptor() {
		return Descriptor.builder();
	}

	public static DocBuilder doc() {
		return Doc.builder();
	}

	public static ExtBuilder ext() {
		return Ext.builder();
	}
}
