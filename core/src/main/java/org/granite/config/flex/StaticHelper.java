package org.granite.config.flex;

import org.granite.util.XMap;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public abstract class StaticHelper {
    public static Adapter getAdapter(XMap element) {
        String id = element.get("@id");
        String className = element.get("@class");
        XMap properties = new XMap(element.getOne("properties"));

        return new Adapter(id, className, properties);
    }

    public static Channel getChannel(XMap element) {
        String id = element.get("@id");
        String className = element.get("@class");

        XMap endPointElt = element.getOne("endpoint");
        if (endPointElt == null)
            throw new RuntimeException(
                    "Excepting a 'endpoint' element in 'channel-definition': " + id);
        EndPoint endPoint = getEndPoint(endPointElt);

        XMap properties = new XMap(element.getOne("properties"));

        return new Channel(id, className, endPoint, properties);
    }


    public static Destination getDestination(XMap element,
                                             Adapter defaultAdapter,
                                             Map<String, Adapter> adaptersMap) {
        String id = element.get("@id");

        List<String> channelRefs = new ArrayList<String>();
        for (XMap channel : element.getAll("channels/channel[@ref]"))
            channelRefs.add(channel.get("@ref"));

        XMap properties = new XMap(element.getOne("properties"));

        List<String> rolesList = null;
        if (element.containsKey("security/security-constraint/roles/role")) {
            rolesList = new ArrayList<String>();
            for (XMap role : element.getAll(
                    "security/security-constraint/roles/role"))
                rolesList.add(role.get("."));
        }

        XMap adapter = element.getOne("adapter[@ref]");
        Adapter adapterRef = adapter != null && adaptersMap != null
                ? adaptersMap.get(adapter.get("@ref"))
                : defaultAdapter;

        return new Destination(id, channelRefs, properties, rolesList,
                               adapterRef, null);
    }

    public static EndPoint getEndPoint(XMap element) {
        String uri = element.get("@uri");
        String className = element.get("@class");

        return new EndPoint(uri, className);
    }

    public static Factory getFactory(XMap element) {
        String id = element.get("@id");
        String className = element.get("@class");
        XMap properties = new XMap(element.getOne("properties"));

        return new Factory(id, className, properties);
    }


    public static Service getService(XMap element) {
        String id = element.get("@id");
        String className = element.get("@class");
        String messageTypes = element.get("@messageTypes");

        Adapter defaultAdapter = null;
        Map<String, Adapter> adaptersMap = new HashMap<String, Adapter>();
        for (XMap adapter : element.getAll("adapters/adapter-definition")) {
            Adapter ad = getAdapter(adapter);
            if (Boolean.TRUE.toString().equals(adapter.get("@default")))
                defaultAdapter = ad;
            adaptersMap.put(ad.getId(), ad);
        }

        Map<String, Destination> destinations = new HashMap<String, Destination>();
        for (XMap destinationElt : element.getAll("destination")) {
            Destination destination = getDestination(destinationElt,
                                                     defaultAdapter,
                                                     adaptersMap);
            destinations.put(destination.getId(), destination);
        }

        return new Service(id, className, messageTypes, defaultAdapter,
                           adaptersMap, destinations);
    }

    public static ServicesConfig getServicesConfig(XMap element) {
        Map<String, Service> finalServices = new HashMap<String, Service>();
        Map<String, Channel> finalChannels = new HashMap<String, Channel>();
        Map<String, Factory> finalFactories = new HashMap<String, Factory>();
        XMap services = element.getOne("services");
        if (services != null) {
            for (XMap service : services.getAll("service")) {
                Service serv = StaticHelper.getService(service);
                finalServices.put(serv.getId(), serv);
            }

            /* TODO: service-include...
            for (Element service : (List<Element>)services.getChildren("service-include")) {
                config.services.add(Service.forElement(service));
            }
            */
        }

        XMap channels = element.getOne("channels");
        if (channels != null) {
            for (XMap channel : channels.getAll("channel-definition")) {
                Channel chan = StaticHelper.getChannel(channel);
                finalChannels.put(chan.getId(), chan);
            }
        } else {
            EndPoint defaultEndpoint = new EndPoint(
                    "http://{server.name}:{server.port}/{context.root}/graniteamf/amf",
                    "flex.messaging.endpoints.AMFEndpoint");
            Channel defaultChannel = new Channel("my-graniteamf",
                                                 "mx.messaging.channels.AMFChannel",
                                                 defaultEndpoint,
                                                 XMap.EMPTY_XMAP);
            finalChannels.put(defaultChannel.getId(), defaultChannel);
        }

        XMap factories = element.getOne("factories");
        if (factories != null) {
            for (XMap factory : factories.getAll("factory")) {
                Factory fact = StaticHelper.getFactory(factory);
                finalFactories.put(fact.getId(), fact);
            }
        }
        return new ServicesConfig(finalServices, finalChannels,
                                  finalFactories);
    }
}