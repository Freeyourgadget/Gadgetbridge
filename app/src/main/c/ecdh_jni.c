#include <jni.h>
#include "ecdh.h"

JNIEXPORT jbyteArray JNICALL
Java_nodomain_freeyourgadget_gadgetbridge_service_devices_huami_operations_InitOperation2021_ecdh_1generate_1public(
        JNIEnv *env, jobject thiz, jbyteArray private_ec) {
    jboolean isCopy;
    jbyte *a = (*env)->GetByteArrayElements(env, private_ec, &isCopy);
    jbyte public_ec[48];
    ecdh_generate_keys((unsigned char *) public_ec, (unsigned char *) a);
    jbyteArray return_array = (*env)->NewByteArray(env, 48);
    (*env)->SetByteArrayRegion(env, return_array, 0, 48, public_ec);
    return return_array;
}