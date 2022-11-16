package com.datasonnet.plugins;

/*-
 * Copyright 2019-2021 the original author or authors.
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

import com.datasonnet.document.DefaultDocument;
import com.datasonnet.document.Document;
import com.datasonnet.document.MediaType;
import com.datasonnet.document.MediaTypes;
import com.datasonnet.spi.AbstractDataFormatPlugin;
import com.datasonnet.spi.PluginException;
import com.datasonnet.spi.ujsonUtils;
import ujson.Value;

import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.file.Path;

import org.apache.commons.io.IOUtils;

public class DefaultJSONFormatPlugin extends AbstractDataFormatPlugin {
    public DefaultJSONFormatPlugin() {
        supportedTypes.add(MediaTypes.APPLICATION_JSON);
        supportedTypes.add(new MediaType("application", "*+json"));

        writerParams.add(DS_PARAM_INDENT);

        readerSupportedClasses.add(java.lang.String.class);
        readerSupportedClasses.add(java.lang.CharSequence.class);
        readerSupportedClasses.add(java.nio.file.Path.class);
        readerSupportedClasses.add(java.io.File.class);
        readerSupportedClasses.add(java.nio.ByteBuffer.class);
        readerSupportedClasses.add(byte[].class);
        readerSupportedClasses.add(java.io.InputStream.class);

        writerSupportedClasses.add(java.lang.String.class);
        writerSupportedClasses.add(java.lang.CharSequence.class);
        writerSupportedClasses.add(java.nio.ByteBuffer.class);
        writerSupportedClasses.add(java.io.OutputStream.class);
        writerSupportedClasses.add(byte[].class);
    }

    @Override
    public Value read(Document<?> doc) throws PluginException {
        if (doc.getContent() == null) {
            return ujson.Null$.MODULE$;
        }

        Class<?> targetType = doc.getContent().getClass();

        if (String.class.isAssignableFrom(targetType)) {
            return ujsonUtils.read(ujson.Readable.fromString((String) doc.getContent()), false);
        }

        if (CharSequence.class.isAssignableFrom(targetType)) {
            return ujsonUtils.read(ujson.Readable.fromCharSequence((CharSequence) doc.getContent()), false);
        }

        if (Path.class.isAssignableFrom(targetType)) {
            return ujsonUtils.read((ujson.Readable) ujson.Readable.fromPath((Path) doc.getContent()), false);
        }

        if (File.class.isAssignableFrom(targetType)) {
            return ujsonUtils.read((ujson.Readable) ujson.Readable.fromFile((File) doc.getContent()), false);
        }

        if (ByteBuffer.class.isAssignableFrom(targetType)) {
            return ujsonUtils.read(ujson.Readable.fromByteBuffer((ByteBuffer) doc.getContent()), false);
        }

        if (byte[].class.isAssignableFrom(targetType)) {
            return ujsonUtils.read(ujson.Readable.fromByteArray((byte[]) doc.getContent()), false);
        }
        
        if (InputStream.class.isAssignableFrom(targetType)) {
        	try (InputStream is = ((InputStream) doc.getContent())) {
        		return ujsonUtils.read(ujson.Readable.fromByteArray(IOUtils.toByteArray(is)), false);
        	} catch(IOException e) {
        		throw new PluginException(e);
        	}
        }

        throw new PluginException(new IllegalArgumentException("Unsupported document content class, use the test method canRead before invoking read"));
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> Document<T> write(Value input, MediaType mediaType, Class<T> targetType) throws PluginException {
        Charset charset = mediaType.getCharset();
        if (charset == null) {
            charset = Charset.defaultCharset();
        }

        int indent = mediaType.getParameters().containsKey(DS_PARAM_INDENT) ? 4 : -1;

        if (targetType.isAssignableFrom(String.class)) {
            return new DefaultDocument<>((T) ujsonUtils.write(input, indent, false), MediaTypes.APPLICATION_JSON);
        }

        if (targetType.isAssignableFrom(CharSequence.class)) {
            return new DefaultDocument<>((T) ujsonUtils.write(input, indent, false), MediaTypes.APPLICATION_JSON);
        }

        if (targetType.isAssignableFrom(OutputStream.class)) {
            BufferedOutputStream out = new BufferedOutputStream(new ByteArrayOutputStream());
            ujsonUtils.writeTo(input, new OutputStreamWriter(out, charset), indent, false);

            return new DefaultDocument<>((T) out, MediaTypes.APPLICATION_JSON);
        }

        if (targetType.isAssignableFrom(ByteBuffer.class)) {
            return new DefaultDocument<>((T) ByteBuffer.wrap(ujsonUtils.write(input, indent, false).getBytes(charset)), MediaTypes.APPLICATION_JSON);
        }

        if (targetType.isAssignableFrom(byte[].class)) {
            return new DefaultDocument<>((T) ujsonUtils.write(input, indent, false).getBytes(charset), MediaTypes.APPLICATION_JSON);
        }

        throw new PluginException(new IllegalArgumentException("Unsupported document content class, use the test method canRead before invoking read"));
    }
}
