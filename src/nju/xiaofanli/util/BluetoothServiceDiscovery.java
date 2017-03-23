package nju.xiaofanli.util;
import javax.bluetooth.*;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * This class is used for discovering the bluetooth service of a ZenWheels Micro Car.
 * <br/>First, turn on one ZenWheels Micro Car and wait for several seconds until the car is ready.
 * Then run the main method to search its service. You may get the output info like this:
 * <p>search services on 000666619F38 MicroCar-56
 * <br/>service RN-SPP found btspp://000666619F38:1;authenticate=false;encrypt=false;master=false
 * <br/>service search completed!
 * <p>Then MicroCar-56 (bluetooth address is 000666619F38)'s service URL should be
 * btspp://000666619F38:1;authenticate=false;encrypt=false;master=false
 * <br/>You should use it to fill in the 'url' attribute of the corresponding car in the file runtime/config.xml.
 */
public class BluetoothServiceDiscovery {
    public static void main(String[] args) throws IOException, InterruptedException {
        List<RemoteDevice> devicesDiscovered = discoverRemoteDevice();
        discoverService(devicesDiscovered);
    }

    private static List<RemoteDevice> discoverRemoteDevice() {
        List<RemoteDevice> devicesDiscovered = new ArrayList<>();
        final Object inquiryCompletedEvent = new Object();

        DiscoveryListener listener = new DiscoveryListener() {

            public void deviceDiscovered(RemoteDevice btDevice, DeviceClass cod) {
                System.out.println("Device " + btDevice.getBluetoothAddress() + " found");
                devicesDiscovered.add(btDevice);
                try {
                    System.out.println("     name " + btDevice.getFriendlyName(false));
                } catch (IOException ignored) {
                }
            }

            public void inquiryCompleted(int discType) {
                System.out.println("Device Inquiry completed!");
                synchronized(inquiryCompletedEvent){
                    inquiryCompletedEvent.notifyAll();
                }
            }

            public void serviceSearchCompleted(int transID, int respCode) {
            }

            public void servicesDiscovered(int transID, ServiceRecord[] servRecord) {
            }
        };

        synchronized(inquiryCompletedEvent) {
            try {
                boolean started = LocalDevice.getLocalDevice().getDiscoveryAgent().startInquiry(DiscoveryAgent.GIAC, listener);
                if (started) {
                    System.out.println("wait for device inquiry to complete...");
                    inquiryCompletedEvent.wait();
                    System.out.println(devicesDiscovered.size() +  " device(s) found");
                }
            } catch (BluetoothStateException | InterruptedException e) {
                e.printStackTrace();
            }
        }
        return devicesDiscovered;
    }

    private static List<String> discoverService(List<RemoteDevice> devicesDiscovered) {
        List<String> serviceFound = new ArrayList<>();
        final Object serviceSearchCompletedEvent = new Object();

        DiscoveryListener listener = new DiscoveryListener() {

            public void deviceDiscovered(RemoteDevice btDevice, DeviceClass cod) {
            }

            public void inquiryCompleted(int discType) {
            }

            public void servicesDiscovered(int transID, ServiceRecord[] servRecord) {
                for (ServiceRecord aServRecord : servRecord) {
                    String url = aServRecord.getConnectionURL(ServiceRecord.NOAUTHENTICATE_NOENCRYPT, false);
                    if (url == null) {
                        continue;
                    }
                    serviceFound.add(url);
                    DataElement serviceName = aServRecord.getAttributeValue(0x0100);
                    if (serviceName != null) {
                        System.out.println("service " + serviceName.getValue() + " found " + url);
                    } else {
                        System.out.println("service found " + url);
                    }
                }
            }

            public void serviceSearchCompleted(int transID, int respCode) {
                System.out.println("service search completed!");
                synchronized(serviceSearchCompletedEvent){
                    serviceSearchCompletedEvent.notifyAll();
                }
            }

        };

        final UUID SERIAL_PORT = new UUID(0x1101);
        UUID[] searchUuidSet = new UUID[] {SERIAL_PORT};
        int[] attrIDs =  new int[] {
                0x0100 // Service name
        };

        for(RemoteDevice btDevice : devicesDiscovered) {
            synchronized(serviceSearchCompletedEvent) {
                try {
                    System.out.println("search services on " + btDevice.getBluetoothAddress() + " " + btDevice.getFriendlyName(false));
                    LocalDevice.getLocalDevice().getDiscoveryAgent().searchServices(attrIDs, searchUuidSet, btDevice, listener);
                    serviceSearchCompletedEvent.wait();
                } catch (InterruptedException | IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return serviceFound;
    }
}
