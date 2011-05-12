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

package org.granite.util {

  import flash.utils.Dictionary;
  import flash.utils.describeType;
  import flash.utils.getDefinitionByName;
  import flash.utils.getQualifiedClassName;
  import flash.net.registerClassAlias;

public  class GraniteClassRegistry {

    private static var destinationClasses: Dictionary = new Dictionary();

    public static function registerClasses(destination:String, classes:Array):void {
          var aliasMap: Dictionary = new Dictionary();

          for each(var clazz: Class in classes)
          {
              aliasMap[getAlias(clazz)] = clazz;
          }

          destinationClasses[destination] = aliasMap;
    }

    public static function unregisterClasses(destination:String):void {
         delete destinationClasses[destination];
    }

    public static function useClasses(destination:String): void {
         var aliasMap: Dictionary = destinationClasses[destination];
         if(aliasMap)
         {
             for(var alias: String in aliasMap)
             {
                 registerClassAlias(alias, aliasMap[alias]);
             }
         }
    }

    public static function getAlias(clazz: Object): String {

        var classInfo:XML = describeType(clazz);
        var remoteClass:XMLList = classInfo.factory.metadata.(@name = "RemoteClass");
        var remoteClassTag:XML;
        var remoteClassAlias:String;
        for each (remoteClassTag in remoteClass) {
            if (remoteClassTag.elements("arg").length() == 1) {
                remoteClassAlias = remoteClassTag.arg.(@key = "alias").@value.toString();
            }
        }

        // If a remote class alias was found use it as a registration name, otherwise use its full qualified class name.
        var registrationName:String;
        if (remoteClassAlias) {
            registrationName = remoteClassAlias;
        }
        else {
           registrationName = getQualifiedClassName(clazz);
        }

        return registrationName;
    }
}
}