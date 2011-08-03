/*
 GRANITE DATA SERVICES
 Copyright (C) 2011 GRANITE DATA SERVICES S.A.S.

 This file is part of Granite Data Services.

 Granite Data Services is free software; you can redistribute it and/or modify
 it under the terms of the GNU Library General Public License as published by
 the Free Software Foundation; either version 2 of the License, or (at your
 option) any later version.

 Granite Data Services is distributed in the hope that it will be useful, but
 WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 FITNESS FOR A PARTICULAR PURPOSE. See the GNU Library General Public License
 for more details.

 You should have received a copy of the GNU Library General Public License
 along with this library; if not, see <http://www.gnu.org/licenses/>.
 */

package org.granite.gravity.channels {
import flash.utils.ByteArray;

import mx.messaging.MessageResponder;
import mx.messaging.messages.IMessage;

import org.granite.util.GraniteClassRegistry;

public class GravityOSGiChannel extends GravityChannel {
    private static const BYTEARRAY_BODY_HEADER:String = "GDS_BYTEARRAY_BODY";

    public function GravityOSGiChannel(id:String, uri:String) {
        super(id, uri);
    }

    override internal function callResponder(responder:MessageResponder, response:IMessage):void {
        if (responder && response.headers[BYTEARRAY_BODY_HEADER] && response.body is ByteArray) {
            GraniteClassRegistry.useClasses(response.destination);
            response.body = ByteArray(response.body).readObject();
            delete response.headers[BYTEARRAY_BODY_HEADER];
        }
        super.callResponder(responder, response);
    }
}
}
