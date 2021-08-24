#include <jni.h>
#include "ecdh.h"

JNIEXPORT jbyteArray JNICALL
Java_nodomain_freeyourgadget_gadgetbridge_service_devices_huami_operations_InitOperation2021_ecdh_1generate_1public(
        JNIEnv *env, jobject thiz, jbyteArray private_ec) {
    jboolean isCopy;
    jbyte *privec = (*env)->GetByteArrayElements(env, private_ec, &isCopy);
    jbyte public_ec[48];
    ecdh_generate_keys((unsigned char *) public_ec, (unsigned char *) privec);
    jbyteArray return_array = (*env)->NewByteArray(env, 48);
    (*env)->SetByteArrayRegion(env, return_array, 0, 48, public_ec);
    return return_array;
}

JNIEXPORT jbyteArray JNICALL
Java_nodomain_freeyourgadget_gadgetbridge_service_devices_huami_operations_InitOperation2021_ecdh_1generate_1shared(
        JNIEnv *env, jobject thiz, jbyteArray private_ec, jbyteArray remote_public_ec) {

    jboolean isCopy;
    jbyte *privec = (*env)->GetByteArrayElements(env, private_ec, &isCopy);
    jbyte *rempubec = (*env)->GetByteArrayElements(env, remote_public_ec, &isCopy);
    jbyte shared_ec[48];

    ecdh_shared_secret((unsigned char *) privec, (unsigned char *) rempubec,
                       (unsigned char *) shared_ec);
    jbyteArray return_array = (*env)->NewByteArray(env, 48);
    (*env)->SetByteArrayRegion(env, return_array, 0, 48, shared_ec);
    return return_array;
}