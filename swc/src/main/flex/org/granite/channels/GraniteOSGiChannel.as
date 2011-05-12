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

package org.granite.channels
{

import mx.messaging.channels.AMFChannel
import mx.messaging.MessageAgent;
import mx.messaging.messages.IMessage;
import mx.messaging.messages.CommandMessage;
import mx.messaging.MessageResponder;
import flash.utils.ByteArray;
import org.granite.util.GraniteClassRegistry;

public class GraniteOSGiChannel extends AMFChannel {

    private static const BYTEARRAY_BODY_HEADER:String = "GDS_BYTEARRAY_BODY";

    public function GraniteOSGiChannel(id:String = null, uri:String = null)
    {
        super(id, uri);
    }

    /**
     *  @private
     */
    override public function send(agent:MessageAgent, message:IMessage):void
    {
        // Pack the body in a ByteArray (avoid direct deserialization on the server)
        if (message && !(message is CommandMessage) && message.body != null) {

            // Register classes following the destination
            var destination : String = message.destination;
            if (destination.length == 0)
                destination = agent.destination;
            GraniteClassRegistry.useClasses(destination);

            message.headers[BYTEARRAY_BODY_HEADER] = true;
            var data:ByteArray = new ByteArray();
            data.writeObject(message.body);
            message.body = data;
        }

        super.send(agent, message);
    }


    override protected function getDefaultMessageResponder(agent:MessageAgent, msg:IMessage):MessageResponder
    {
        return new OSGiMessageResponder(agent, msg, this);
    }
}
}