package stest.tron.wallet.dailybuild.multisign;

import static org.tron.api.GrpcAPI.Return.response_code.CONTRACT_VALIDATE_ERROR;

import com.google.protobuf.ByteString;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import org.junit.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Test;
import org.tron.api.GrpcAPI;
import org.tron.api.WalletGrpc;
import org.tron.common.crypto.ECKey;
import org.tron.common.utils.ByteArray;
import org.tron.common.utils.Utils;
import org.tron.core.Wallet;
import stest.tron.wallet.common.client.Configuration;
import stest.tron.wallet.common.client.Parameter.CommonConstant;
import stest.tron.wallet.common.client.WalletClient;
import stest.tron.wallet.common.client.utils.PublicMethedForDailybuild;
import stest.tron.wallet.common.client.utils.PublicMethedForMutiSign;

@Slf4j
public class MultiSign13 {

  private final String testKey002 = Configuration.getByPath("testng.conf")
      .getString("foundationAccount.key1");
  private final byte[] fromAddress = PublicMethedForDailybuild.getFinalAddress(testKey002);

  private final String witnessKey001 = Configuration.getByPath("testng.conf")
      .getString("witness.key1");
  private final byte[] witnessAddress001 = PublicMethedForDailybuild.getFinalAddress(witnessKey001);

  private long multiSignFee = Configuration.getByPath("testng.conf")
      .getLong("defaultParameter.multiSignFee");
  private long updateAccountPermissionFee = Configuration.getByPath("testng.conf")
      .getLong("defaultParameter.updateAccountPermissionFee");

  private ECKey ecKey1 = new ECKey(Utils.getRandom());
  private byte[] ownerAddress = ecKey1.getAddress();
  private String ownerKey = ByteArray.toHexString(ecKey1.getPrivKeyBytes());

  private ECKey ecKey2 = new ECKey(Utils.getRandom());
  private byte[] normalAddr001 = ecKey2.getAddress();
  private String normalKey001 = ByteArray.toHexString(ecKey2.getPrivKeyBytes());

  private ECKey tmpEcKey01 = new ECKey(Utils.getRandom());
  private byte[] tmpAddr01 = tmpEcKey01.getAddress();
  private String tmpKey01 = ByteArray.toHexString(tmpEcKey01.getPrivKeyBytes());

  private ECKey tmpEcKey02 = new ECKey(Utils.getRandom());
  private byte[] tmpAddr02 = tmpEcKey02.getAddress();
  private String tmpKey02 = ByteArray.toHexString(tmpEcKey02.getPrivKeyBytes());

  private ManagedChannel channelFull = null;
  private WalletGrpc.WalletBlockingStub blockingStubFull = null;
  private String fullnode = Configuration.getByPath("testng.conf")
      .getStringList("fullnode.ip.list").get(1);
  private long maxFeeLimit = Configuration.getByPath("testng.conf")
      .getLong("defaultParameter.maxFeeLimit");

  private static final long now = System.currentTimeMillis();
  private static String tokenName = "testAssetIssue_" + Long.toString(now);
  private static ByteString assetAccountId = null;
  private static final long TotalSupply = 1000L;
  private byte[] transferTokenContractAddress = null;

  private String description = Configuration.getByPath("testng.conf")
      .getString("defaultParameter.assetDescription");
  private String url = Configuration.getByPath("testng.conf")
      .getString("defaultParameter.assetUrl");


  @BeforeSuite
  public void beforeSuite() {
    Wallet wallet = new Wallet();
    Wallet.setAddressPreFixByte(CommonConstant.ADD_PRE_FIX_BYTE_MAINNET);
  }

  /**
   * constructor.
   */
  @BeforeClass(enabled = true)
  public void beforeClass() {

    channelFull = ManagedChannelBuilder.forTarget(fullnode)
        .usePlaintext(true)
        .build();
    blockingStubFull = WalletGrpc.newBlockingStub(channelFull);
  }

  @Test(enabled = true, description = "Witness permission_name is witness")
  public void testWitnessName01() {
    String ownerKey = witnessKey001;
    byte[] ownerAddress = new WalletClient(ownerKey).getAddress();
    long needCoin = updateAccountPermissionFee * 2;

    PublicMethedForDailybuild
        .sendcoin(ownerAddress, needCoin, fromAddress, testKey002, blockingStubFull);

    Assert.assertTrue(PublicMethedForDailybuild
        .freezeBalanceForReceiver(fromAddress, 1000000000, 0, 0, ByteString.copyFrom(ownerAddress),
            testKey002, blockingStubFull));

    PublicMethedForDailybuild.waitProduceNextBlock(blockingStubFull);
    PublicMethedForDailybuild.waitProduceNextBlock(blockingStubFull);

    Long balanceBefore = PublicMethedForDailybuild.queryAccount(ownerAddress, blockingStubFull)
        .getBalance();
    logger.info("balanceBefore: " + balanceBefore);
    PublicMethedForDailybuild.printAddress(ownerKey);
    PublicMethedForDailybuild.printAddress(tmpKey02);

    List<String> ownerPermissionKeys = new ArrayList<>();

    ownerPermissionKeys.add(ownerKey);

    logger.info("** update owner and active permission to two address");
    String accountPermissionJson =
        "{\"owner_permission\":{\"type\":0,\"permission_name\":\"owner\",\"threshold\":1,\"keys\":["
            + "{\"address\":\"" + PublicMethedForDailybuild.getAddressString(testKey002)
            + "\",\"weight\":1}]},"
            + "\"witness_permission\":{\"type\":1,\"permission_name\":\"witness\","
            + "\"threshold\":1,\"keys\":["
            + "{\"address\":\"" + PublicMethedForDailybuild.getAddressString(tmpKey02)
            + "\",\"weight\":1}]},"
            + "\"active_permissions\":[{\"type\":2,\"permission_name\":\"active\",\"threshold\":1,"
            + "\"operations\":\"3f3d1ec0032001000000000000000000000000000000000000000000000000c0\","
            + "\"keys\":["
            + "{\"address\":\"" + PublicMethedForDailybuild.getAddressString(witnessKey001) + "\",\"weight\":1},"
            + "{\"address\":\"" + PublicMethedForDailybuild.getAddressString(tmpKey02) + "\",\"weight\":1}"
            + "]}]}";

    Assert.assertTrue(PublicMethedForMutiSign.accountPermissionUpdate(accountPermissionJson,
        ownerAddress, ownerKey, blockingStubFull,
        ownerPermissionKeys.toArray(new String[ownerPermissionKeys.size()])));
    PublicMethedForDailybuild.waitProduceNextBlock(blockingStubFull);

    ownerPermissionKeys.clear();
    ownerPermissionKeys.add(testKey002);

    Assert.assertEquals(2,
        PublicMethedForMutiSign.getActivePermissionKeyCount(PublicMethedForDailybuild.queryAccount(ownerAddress,
            blockingStubFull).getActivePermissionList()));

    Assert.assertEquals(1, PublicMethedForDailybuild.queryAccount(ownerAddress,
        blockingStubFull).getOwnerPermission().getKeysCount());

    Assert.assertEquals(1, PublicMethedForDailybuild.queryAccount(ownerAddress,
        blockingStubFull).getWitnessPermission().getKeysCount());

    PublicMethedForMutiSign.printPermissionList(PublicMethedForDailybuild.queryAccount(ownerAddress,
        blockingStubFull).getActivePermissionList());

    System.out
        .printf(PublicMethedForMutiSign.printPermission(PublicMethedForDailybuild.queryAccount(ownerAddress,
            blockingStubFull).getOwnerPermission()));

    System.out
        .printf(PublicMethedForMutiSign.printPermission(PublicMethedForDailybuild.queryAccount(ownerAddress,
            blockingStubFull).getWitnessPermission()));

    PublicMethedForMutiSign
        .recoverWitnessPermission(ownerKey, ownerPermissionKeys, blockingStubFull);
    PublicMethedForDailybuild.waitProduceNextBlock(blockingStubFull);
    Long balanceAfter = PublicMethedForDailybuild.queryAccount(ownerAddress, blockingStubFull)
        .getBalance();
    logger.info("balanceAfter: " + balanceAfter);

    Assert.assertEquals(balanceBefore - balanceAfter, needCoin);

    PublicMethedForDailybuild
        .unFreezeBalance(fromAddress, testKey002, 0, ownerAddress, blockingStubFull);
  }

  @Test(enabled = true, description = "Witness permission_name is witness12")
  public void testWitnessName02() {
    String ownerKey = witnessKey001;
    byte[] ownerAddress = new WalletClient(ownerKey).getAddress();
    long needCoin = updateAccountPermissionFee * 2;

    PublicMethedForDailybuild.sendcoin(ownerAddress, needCoin, fromAddress, testKey002, blockingStubFull);
    Assert.assertTrue(PublicMethedForDailybuild
        .freezeBalanceForReceiver(fromAddress, 1000000000L, 0, 0, ByteString.copyFrom(ownerAddress),
            testKey002, blockingStubFull));
    PublicMethedForDailybuild.waitProduceNextBlock(blockingStubFull);
    Long balanceBefore = PublicMethedForDailybuild.queryAccount(ownerAddress, blockingStubFull)
        .getBalance();
    logger.info("balanceBefore: " + balanceBefore);
    PublicMethedForDailybuild.printAddress(ownerKey);
    PublicMethedForDailybuild.printAddress(tmpKey02);

    List<String> ownerPermissionKeys = new ArrayList<>();

    ownerPermissionKeys.add(ownerKey);

    logger.info("** update owner and active permission to two address");
    String accountPermissionJson =
        "{\"owner_permission\":{\"type\":0,\"permission_name\":\"owner\",\"threshold\":1,\"keys\":["
            + "{\"address\":\"" + PublicMethedForDailybuild.getAddressString(testKey002)
            + "\",\"weight\":1}]},"
            + "\"witness_permission\":{\"type\":1,\"permission_name\":\"witness12\","
            + "\"threshold\":1,\"keys\":["
            + "{\"address\":\"" + PublicMethedForDailybuild.getAddressString(tmpKey02)
            + "\",\"weight\":1}]},"
            + "\"active_permissions\":[{\"type\":2,\"permission_name\":\"active\",\"threshold\":1,"
            + "\"operations\":\"3f3d1ec0032001000000000000000000000000000000000000000000000000c0\","
            + "\"keys\":["
            + "{\"address\":\"" + PublicMethedForDailybuild.getAddressString(witnessKey001) + "\",\"weight\":1},"
            + "{\"address\":\"" + PublicMethedForDailybuild.getAddressString(tmpKey02) + "\",\"weight\":1}"
            + "]}]}";

    Assert.assertTrue(PublicMethedForMutiSign.accountPermissionUpdate(accountPermissionJson,
        ownerAddress, ownerKey, blockingStubFull,
        ownerPermissionKeys.toArray(new String[ownerPermissionKeys.size()])));

    PublicMethedForDailybuild.waitProduceNextBlock(blockingStubFull);

    ownerPermissionKeys.clear();
    ownerPermissionKeys.add(testKey002);

    Assert.assertEquals(2,
        PublicMethedForMutiSign.getActivePermissionKeyCount(PublicMethedForDailybuild.queryAccount(ownerAddress,
            blockingStubFull).getActivePermissionList()));

    Assert.assertEquals(1, PublicMethedForDailybuild.queryAccount(ownerAddress,
        blockingStubFull).getOwnerPermission().getKeysCount());

    Assert.assertEquals(1, PublicMethedForDailybuild.queryAccount(ownerAddress,
        blockingStubFull).getWitnessPermission().getKeysCount());

    PublicMethedForMutiSign.printPermissionList(PublicMethedForDailybuild.queryAccount(ownerAddress,
        blockingStubFull).getActivePermissionList());

    System.out
        .printf(PublicMethedForMutiSign.printPermission(PublicMethedForDailybuild.queryAccount(ownerAddress,
            blockingStubFull).getOwnerPermission()));

    System.out
        .printf(PublicMethedForMutiSign.printPermission(PublicMethedForDailybuild.queryAccount(ownerAddress,
            blockingStubFull).getWitnessPermission()));

    PublicMethedForMutiSign
        .recoverWitnessPermission(ownerKey, ownerPermissionKeys, blockingStubFull);

    Long balanceAfter = PublicMethedForDailybuild.queryAccount(ownerAddress, blockingStubFull)
        .getBalance();
    logger.info("balanceAfter: " + balanceAfter);
    Assert.assertEquals(balanceBefore - balanceAfter, needCoin);

    PublicMethedForDailybuild
        .unFreezeBalance(fromAddress, testKey002, 0, ownerAddress, blockingStubFull);
  }

  @Test(enabled = true, description = "Witness permission_name is \"123\"")
  public void testWitnessName03() {
    String ownerKey = witnessKey001;
    byte[] ownerAddress = new WalletClient(ownerKey).getAddress();
    long needCoin = updateAccountPermissionFee * 2;

    PublicMethedForDailybuild.sendcoin(ownerAddress, needCoin, fromAddress, testKey002, blockingStubFull);

    Assert.assertTrue(PublicMethedForDailybuild
        .freezeBalanceForReceiver(fromAddress, 1000000000, 0, 0, ByteString.copyFrom(ownerAddress),
            testKey002, blockingStubFull));

    PublicMethedForDailybuild.waitProduceNextBlock(blockingStubFull);
    Long balanceBefore = PublicMethedForDailybuild.queryAccount(ownerAddress, blockingStubFull)
        .getBalance();
    logger.info("balanceBefore: " + balanceBefore);
    PublicMethedForDailybuild.printAddress(ownerKey);
    PublicMethedForDailybuild.printAddress(tmpKey02);

    List<String> ownerPermissionKeys = new ArrayList<>();

    ownerPermissionKeys.add(ownerKey);

    logger.info("** update owner and active permission to two address");
    String accountPermissionJson =
        "{\"owner_permission\":{\"type\":0,\"permission_name\":\"owner\",\"threshold\":1,\"keys\":["
            + "{\"address\":\"" + PublicMethedForDailybuild.getAddressString(testKey002)
            + "\",\"weight\":1}]},"
            + "\"witness_permission\":{\"type\":1,\"permission_name\":\"123\","
            + "\"threshold\":1,\"keys\":["
            + "{\"address\":\"" + PublicMethedForDailybuild.getAddressString(tmpKey02)
            + "\",\"weight\":1}]},"
            + "\"active_permissions\":[{\"type\":2,\"permission_name\":\"active\",\"threshold\":1,"
            + "\"operations\":\"3f3d1ec0032001000000000000000000000000000000000000000000000000c0\","
            + "\"keys\":["
            + "{\"address\":\"" + PublicMethedForDailybuild.getAddressString(witnessKey001) + "\",\"weight\":1},"
            + "{\"address\":\"" + PublicMethedForDailybuild.getAddressString(tmpKey02) + "\",\"weight\":1}"
            + "]}]}";

    Assert.assertTrue(PublicMethedForMutiSign.accountPermissionUpdate(accountPermissionJson,
        ownerAddress, ownerKey, blockingStubFull,
        ownerPermissionKeys.toArray(new String[ownerPermissionKeys.size()])));

    PublicMethedForDailybuild.waitProduceNextBlock(blockingStubFull);

    ownerPermissionKeys.clear();
    ownerPermissionKeys.add(testKey002);

    Assert.assertEquals(2,
        PublicMethedForMutiSign.getActivePermissionKeyCount(PublicMethedForDailybuild.queryAccount(ownerAddress,
            blockingStubFull).getActivePermissionList()));

    Assert.assertEquals(1, PublicMethedForDailybuild.queryAccount(ownerAddress,
        blockingStubFull).getOwnerPermission().getKeysCount());

    Assert.assertEquals(1, PublicMethedForDailybuild.queryAccount(ownerAddress,
        blockingStubFull).getWitnessPermission().getKeysCount());

    PublicMethedForMutiSign.printPermissionList(PublicMethedForDailybuild.queryAccount(ownerAddress,
        blockingStubFull).getActivePermissionList());

    System.out
        .printf(PublicMethedForMutiSign.printPermission(PublicMethedForDailybuild.queryAccount(ownerAddress,
            blockingStubFull).getOwnerPermission()));

    System.out
        .printf(PublicMethedForMutiSign.printPermission(PublicMethedForDailybuild.queryAccount(ownerAddress,
            blockingStubFull).getWitnessPermission()));

    PublicMethedForMutiSign
        .recoverWitnessPermission(ownerKey, ownerPermissionKeys, blockingStubFull);

    Long balanceAfter = PublicMethedForDailybuild.queryAccount(ownerAddress, blockingStubFull)
        .getBalance();
    logger.info("balanceAfter: " + balanceAfter);
    Assert.assertEquals(balanceBefore - balanceAfter, needCoin);

    PublicMethedForDailybuild
        .unFreezeBalance(fromAddress, testKey002, 0, ownerAddress, blockingStubFull);
  }

  @Test(enabled = true, description = "Witness permission_name is \"\"")
  public void testWitnessName04() {
    String ownerKey = witnessKey001;
    byte[] ownerAddress = new WalletClient(ownerKey).getAddress();
    long needCoin = updateAccountPermissionFee * 2;

    PublicMethedForDailybuild.sendcoin(ownerAddress, needCoin, fromAddress, testKey002, blockingStubFull);

    Assert.assertTrue(PublicMethedForDailybuild
        .freezeBalanceForReceiver(fromAddress, 1000000000, 0, 0, ByteString.copyFrom(ownerAddress),
            testKey002, blockingStubFull));

    PublicMethedForDailybuild.waitProduceNextBlock(blockingStubFull);
    Long balanceBefore = PublicMethedForDailybuild.queryAccount(ownerAddress, blockingStubFull)
        .getBalance();
    logger.info("balanceBefore: " + balanceBefore);
    List<String> ownerPermissionKeys = new ArrayList<>();

    PublicMethedForDailybuild.printAddress(ownerKey);
    PublicMethedForDailybuild.printAddress(tmpKey02);

    ownerPermissionKeys.add(ownerKey);

    logger.info("** update owner and active permission to two address");
    String accountPermissionJson =
        "{\"owner_permission\":{\"type\":0,\"permission_name\":\"owner\",\"threshold\":1,\"keys\":["
            + "{\"address\":\"" + PublicMethedForDailybuild.getAddressString(testKey002)
            + "\",\"weight\":1}]},"
            + "\"witness_permission\":{\"type\":1,\"permission_name\":\"\","
            + "\"threshold\":1,\"keys\":["
            + "{\"address\":\"" + PublicMethedForDailybuild.getAddressString(tmpKey02)
            + "\",\"weight\":1}]},"
            + "\"active_permissions\":[{\"type\":2,\"permission_name\":\"active\",\"threshold\":1,"
            + "\"operations\":\"3f3d1ec0032001000000000000000000000000000000000000000000000000c0\","
            + "\"keys\":["
            + "{\"address\":\"" + PublicMethedForDailybuild.getAddressString(witnessKey001) + "\",\"weight\":1},"
            + "{\"address\":\"" + PublicMethedForDailybuild.getAddressString(tmpKey02) + "\",\"weight\":1}"
            + "]}]}";

    Assert.assertTrue(PublicMethedForMutiSign.accountPermissionUpdate(accountPermissionJson,
        ownerAddress, ownerKey, blockingStubFull,
        ownerPermissionKeys.toArray(new String[ownerPermissionKeys.size()])));

    PublicMethedForDailybuild.waitProduceNextBlock(blockingStubFull);

    ownerPermissionKeys.clear();
    ownerPermissionKeys.add(testKey002);

    Assert.assertEquals(2, PublicMethedForMutiSign.getActivePermissionKeyCount(
        PublicMethedForDailybuild.queryAccount(ownerAddress, blockingStubFull).getActivePermissionList()));

    Assert.assertEquals(1, PublicMethedForDailybuild.queryAccount(ownerAddress,
        blockingStubFull).getOwnerPermission().getKeysCount());

    Assert.assertEquals(1, PublicMethedForDailybuild.queryAccount(ownerAddress,
        blockingStubFull).getWitnessPermission().getKeysCount());

    PublicMethedForMutiSign.printPermissionList(PublicMethedForDailybuild.queryAccount(ownerAddress,
        blockingStubFull).getActivePermissionList());

    System.out
        .printf(PublicMethedForMutiSign.printPermission(PublicMethedForDailybuild.queryAccount(ownerAddress,
            blockingStubFull).getOwnerPermission()));

    System.out
        .printf(PublicMethedForMutiSign.printPermission(PublicMethedForDailybuild.queryAccount(ownerAddress,
            blockingStubFull).getWitnessPermission()));

    PublicMethedForMutiSign
        .recoverWitnessPermission(ownerKey, ownerPermissionKeys, blockingStubFull);

    Long balanceAfter = PublicMethedForDailybuild.queryAccount(ownerAddress, blockingStubFull)
        .getBalance();
    logger.info("balanceAfter: " + balanceAfter);
    Assert.assertEquals(balanceBefore - balanceAfter, needCoin);
    PublicMethedForDailybuild
        .unFreezeBalance(fromAddress, testKey002, 0, ownerAddress, blockingStubFull);

  }

  @Test(enabled = true, description = "Witness permission_name is null")
  public void testWitnessName05() {
    String ownerKey = witnessKey001;
    byte[] ownerAddress = new WalletClient(ownerKey).getAddress();
    PublicMethedForDailybuild.sendcoin(ownerAddress, 1_000000, fromAddress, testKey002, blockingStubFull);

    Assert.assertTrue(PublicMethedForDailybuild
        .freezeBalanceForReceiver(fromAddress, 1000000000, 0, 0, ByteString.copyFrom(ownerAddress),
            testKey002, blockingStubFull));

    PublicMethedForDailybuild.waitProduceNextBlock(blockingStubFull);
    Long balanceBefore = PublicMethedForDailybuild.queryAccount(ownerAddress, blockingStubFull)
        .getBalance();
    logger.info("balanceBefore: " + balanceBefore);
    List<String> ownerPermissionKeys = new ArrayList<>();

    PublicMethedForDailybuild.printAddress(ownerKey);
    PublicMethedForDailybuild.printAddress(tmpKey02);

    ownerPermissionKeys.add(ownerKey);

    String accountPermissionJson =
        "{\"owner_permission\":{\"type\":0,\"permission_name\":\"owner\",\"threshold\":1,\"keys\":["
            + "{\"address\":\"" + PublicMethedForDailybuild.getAddressString(testKey002)
            + "\",\"weight\":1}]},"
            + "\"witness_permission\":{\"type\":1,\"permission_name\":" + null
            + ",\"threshold\":1,\"keys\":["
            + "{\"address\":\"" + PublicMethedForDailybuild.getAddressString(tmpKey02)
            + "\",\"weight\":1}]},"
            + "\"active_permissions\":[{\"type\":2,\"permission_name\":\"active\",\"threshold\":1,"
            + "\"operations\":\"3f3d1ec0032001000000000000000000000000000000000000000000000000c0\","
            + "\"keys\":["
            + "{\"address\":\"" + PublicMethedForDailybuild.getAddressString(witnessKey001) + "\",\"weight\":1},"
            + "{\"address\":\"" + PublicMethedForDailybuild.getAddressString(tmpKey02) + "\",\"weight\":1}"
            + "]}]}";

    boolean ret = false;
    try {
      GrpcAPI.Return response = PublicMethedForDailybuild
          .accountPermissionUpdateForResponse(accountPermissionJson,
              ownerAddress, ownerKey, blockingStubFull);
    } catch (NullPointerException e) {
      logger.info("Expected NullPointerException!");
      ret = true;
    }
    Assert.assertTrue(ret);

    Long balanceAfter = PublicMethedForDailybuild.queryAccount(ownerAddress, blockingStubFull)
        .getBalance();
    logger.info("balanceAfter: " + balanceAfter);
    Assert.assertEquals(balanceBefore, balanceAfter);

    PublicMethedForDailybuild
        .unFreezeBalance(fromAddress, testKey002, 0, ownerAddress, blockingStubFull);

  }

  @Test(enabled = true, description = "Witness doesn't have permission_name")
  public void testWitnessName06() {
    String ownerKey = witnessKey001;
    byte[] ownerAddress = new WalletClient(ownerKey).getAddress();
    long needCoin = updateAccountPermissionFee * 2;

    PublicMethedForDailybuild.sendcoin(ownerAddress, needCoin, fromAddress, testKey002, blockingStubFull);
    Assert.assertTrue(PublicMethedForDailybuild
        .freezeBalanceForReceiver(fromAddress, 1000000000, 0, 0, ByteString.copyFrom(ownerAddress),
            testKey002, blockingStubFull));
    PublicMethedForDailybuild.waitProduceNextBlock(blockingStubFull);
    Long balanceBefore = PublicMethedForDailybuild.queryAccount(ownerAddress, blockingStubFull)
        .getBalance();
    logger.info("balanceBefore: " + balanceBefore);
    List<String> ownerPermissionKeys = new ArrayList<>();

    PublicMethedForDailybuild.printAddress(ownerKey);
    PublicMethedForDailybuild.printAddress(tmpKey02);

    ownerPermissionKeys.add(ownerKey);

    String accountPermissionJson =
        "{\"owner_permission\":{\"type\":0,\"permission_name\":\"owner\",\"threshold\":1,\"keys\":["
            + "{\"address\":\"" + PublicMethedForDailybuild.getAddressString(testKey002)
            + "\",\"weight\":1}]},"
            + "\"witness_permission\":{\"type\":1,\"threshold\":1,\"keys\":["
            + "{\"address\":\"" + PublicMethedForDailybuild.getAddressString(tmpKey02)
            + "\",\"weight\":1}]},"
            + "\"active_permissions\":[{\"type\":2,\"permission_name\":\"active\",\"threshold\":1,"
            + "\"operations\":\"3f3d1ec0032001000000000000000000000000000000000000000000000000c0\","
            + "\"keys\":["
            + "{\"address\":\"" + PublicMethedForDailybuild.getAddressString(witnessKey001) + "\",\"weight\":1},"
            + "{\"address\":\"" + PublicMethedForDailybuild.getAddressString(tmpKey02) + "\",\"weight\":1}"
            + "]}]}";
    Assert.assertTrue(PublicMethedForMutiSign.accountPermissionUpdate(accountPermissionJson,
        ownerAddress, ownerKey, blockingStubFull,
        ownerPermissionKeys.toArray(new String[ownerPermissionKeys.size()])));

    PublicMethedForDailybuild.waitProduceNextBlock(blockingStubFull);

    ownerPermissionKeys.clear();
    ownerPermissionKeys.add(testKey002);

    Assert.assertEquals(2, PublicMethedForMutiSign.getActivePermissionKeyCount(
        PublicMethedForDailybuild.queryAccount(ownerAddress, blockingStubFull).getActivePermissionList()));

    Assert.assertEquals(1, PublicMethedForDailybuild.queryAccount(ownerAddress,
        blockingStubFull).getOwnerPermission().getKeysCount());

    Assert.assertEquals(1, PublicMethedForDailybuild.queryAccount(ownerAddress,
        blockingStubFull).getWitnessPermission().getKeysCount());

    PublicMethedForMutiSign.printPermissionList(PublicMethedForDailybuild.queryAccount(ownerAddress,
        blockingStubFull).getActivePermissionList());

    System.out
        .printf(PublicMethedForMutiSign.printPermission(PublicMethedForDailybuild.queryAccount(ownerAddress,
            blockingStubFull).getOwnerPermission()));

    System.out
        .printf(PublicMethedForMutiSign.printPermission(PublicMethedForDailybuild.queryAccount(ownerAddress,
            blockingStubFull).getWitnessPermission()));

    PublicMethedForMutiSign
        .recoverWitnessPermission(ownerKey, ownerPermissionKeys, blockingStubFull);

    PublicMethedForDailybuild.waitProduceNextBlock(blockingStubFull);
    Long balanceAfter = PublicMethedForDailybuild.queryAccount(ownerAddress, blockingStubFull)
        .getBalance();
    logger.info("balanceAfter: " + balanceAfter);
    Assert.assertEquals(balanceBefore - balanceAfter, needCoin);

    PublicMethedForDailybuild
        .unFreezeBalance(fromAddress, testKey002, 0, ownerAddress, blockingStubFull);

  }

  @Test(enabled = true, description = "Witness permission_name is 123")
  public void testWitnessName07() {
    String ownerKey = witnessKey001;
    byte[] ownerAddress = new WalletClient(ownerKey).getAddress();
    long needCoin = updateAccountPermissionFee * 2;

    PublicMethedForDailybuild.sendcoin(ownerAddress, needCoin, fromAddress, testKey002, blockingStubFull);
    Assert.assertTrue(PublicMethedForDailybuild
        .freezeBalanceForReceiver(fromAddress, 1000000000, 0, 0, ByteString.copyFrom(ownerAddress),
            testKey002, blockingStubFull));
    PublicMethedForDailybuild.waitProduceNextBlock(blockingStubFull);
    Long balanceBefore = PublicMethedForDailybuild.queryAccount(ownerAddress, blockingStubFull)
        .getBalance();
    logger.info("balanceBefore: " + balanceBefore);
    List<String> ownerPermissionKeys = new ArrayList<>();

    PublicMethedForDailybuild.printAddress(ownerKey);
    PublicMethedForDailybuild.printAddress(tmpKey02);

    ownerPermissionKeys.add(ownerKey);

    String accountPermissionJson =
        "{\"owner_permission\":{\"type\":0,\"permission_name\":\"owner\",\"threshold\":1,\"keys\":["
            + "{\"address\":\"" + PublicMethedForDailybuild.getAddressString(testKey002)
            + "\",\"weight\":1}]},"
            + "\"witness_permission\":{\"permission_name\":123,\"type\":1,"
            + "\"threshold\":1,\"keys\":["
            + "{\"address\":\"" + PublicMethedForDailybuild.getAddressString(tmpKey02)
            + "\",\"weight\":1}]},"
            + "\"active_permissions\":[{\"type\":2,\"permission_name\":\"active\",\"threshold\":1,"
            + "\"operations\":\"3f3d1ec0032001000000000000000000000000000000000000000000000000c0\","
            + "\"keys\":["
            + "{\"address\":\"" + PublicMethedForDailybuild.getAddressString(witnessKey001) + "\",\"weight\":1},"
            + "{\"address\":\"" + PublicMethedForDailybuild.getAddressString(tmpKey02) + "\",\"weight\":1}"
            + "]}]}";

    Assert.assertTrue(PublicMethedForMutiSign.accountPermissionUpdate(accountPermissionJson,
        ownerAddress, ownerKey, blockingStubFull,
        ownerPermissionKeys.toArray(new String[ownerPermissionKeys.size()])));

    PublicMethedForDailybuild.waitProduceNextBlock(blockingStubFull);

    ownerPermissionKeys.clear();
    ownerPermissionKeys.add(testKey002);

    Assert.assertEquals(2, PublicMethedForMutiSign.getActivePermissionKeyCount(
        PublicMethedForDailybuild.queryAccount(ownerAddress, blockingStubFull).getActivePermissionList()));

    Assert.assertEquals(1, PublicMethedForDailybuild.queryAccount(ownerAddress,
        blockingStubFull).getOwnerPermission().getKeysCount());

    Assert.assertEquals(1, PublicMethedForDailybuild.queryAccount(ownerAddress,
        blockingStubFull).getWitnessPermission().getKeysCount());

    PublicMethedForMutiSign.printPermissionList(PublicMethedForDailybuild.queryAccount(ownerAddress,
        blockingStubFull).getActivePermissionList());

    System.out
        .printf(PublicMethedForMutiSign.printPermission(PublicMethedForDailybuild.queryAccount(ownerAddress,
            blockingStubFull).getOwnerPermission()));

    System.out
        .printf(PublicMethedForMutiSign.printPermission(PublicMethedForDailybuild.queryAccount(ownerAddress,
            blockingStubFull).getWitnessPermission()));

    PublicMethedForMutiSign
        .recoverWitnessPermission(ownerKey, ownerPermissionKeys, blockingStubFull);

    PublicMethedForDailybuild.waitProduceNextBlock(blockingStubFull);

    Long balanceAfter = PublicMethedForDailybuild.queryAccount(ownerAddress, blockingStubFull)
        .getBalance();
    logger.info("balanceAfter: " + balanceAfter);
    Assert.assertEquals(balanceBefore - balanceAfter, needCoin);
    PublicMethedForDailybuild
        .unFreezeBalance(fromAddress, testKey002, 0, ownerAddress, blockingStubFull);
  }

  @Test(enabled = true, description = "Witness permission_name is 0.1")
  public void testWitnessName08() {
    String ownerKey = witnessKey001;
    byte[] ownerAddress = new WalletClient(ownerKey).getAddress();
    long needCoin = updateAccountPermissionFee * 2;

    PublicMethedForDailybuild.sendcoin(ownerAddress, needCoin, fromAddress, testKey002, blockingStubFull);
    Assert.assertTrue(PublicMethedForDailybuild
        .freezeBalanceForReceiver(fromAddress, 1000000000, 0, 0, ByteString.copyFrom(ownerAddress),
            testKey002, blockingStubFull));
    PublicMethedForDailybuild.waitProduceNextBlock(blockingStubFull);
    Long balanceBefore = PublicMethedForDailybuild.queryAccount(ownerAddress, blockingStubFull)
        .getBalance();
    logger.info("balanceBefore: " + balanceBefore);
    List<String> ownerPermissionKeys = new ArrayList<>();

    PublicMethedForDailybuild.printAddress(ownerKey);
    PublicMethedForDailybuild.printAddress(tmpKey02);

    ownerPermissionKeys.add(ownerKey);

    String accountPermissionJson =
        "{\"owner_permission\":{\"type\":0,\"permission_name\":\"owner\",\"threshold\":1,\"keys\":["
            + "{\"address\":\"" + PublicMethedForDailybuild.getAddressString(testKey002)
            + "\",\"weight\":1}]},"
            + "\"witness_permission\":{\"permission_name\":\"0.1\",\"type\":1,"
            + "\"threshold\":1,\"keys\":["
            + "{\"address\":\"" + PublicMethedForDailybuild.getAddressString(tmpKey02)
            + "\",\"weight\":1}]},"
            + "\"active_permissions\":[{\"type\":2,\"permission_name\":\"active\",\"threshold\":1,"
            + "\"operations\":\"3f3d1ec0032001000000000000000000000000000000000000000000000000c0\","
            + "\"keys\":["
            + "{\"address\":\"" + PublicMethedForDailybuild.getAddressString(witnessKey001) + "\",\"weight\":1},"
            + "{\"address\":\"" + PublicMethedForDailybuild.getAddressString(tmpKey02) + "\",\"weight\":1}"
            + "]}]}";

    Assert.assertTrue(PublicMethedForMutiSign.accountPermissionUpdate(accountPermissionJson,
        ownerAddress, ownerKey, blockingStubFull,
        ownerPermissionKeys.toArray(new String[ownerPermissionKeys.size()])));

    PublicMethedForDailybuild.waitProduceNextBlock(blockingStubFull);

    ownerPermissionKeys.clear();
    ownerPermissionKeys.add(testKey002);

    Assert.assertEquals(2, PublicMethedForMutiSign.getActivePermissionKeyCount(
        PublicMethedForDailybuild.queryAccount(ownerAddress, blockingStubFull).getActivePermissionList()));

    Assert.assertEquals(1, PublicMethedForDailybuild.queryAccount(ownerAddress,
        blockingStubFull).getOwnerPermission().getKeysCount());

    Assert.assertEquals(1, PublicMethedForDailybuild.queryAccount(ownerAddress,
        blockingStubFull).getWitnessPermission().getKeysCount());

    PublicMethedForMutiSign.printPermissionList(PublicMethedForDailybuild.queryAccount(ownerAddress,
        blockingStubFull).getActivePermissionList());

    System.out
        .printf(PublicMethedForMutiSign.printPermission(PublicMethedForDailybuild.queryAccount(ownerAddress,
            blockingStubFull).getOwnerPermission()));

    System.out
        .printf(PublicMethedForMutiSign.printPermission(PublicMethedForDailybuild.queryAccount(ownerAddress,
            blockingStubFull).getWitnessPermission()));

    PublicMethedForMutiSign
        .recoverWitnessPermission(ownerKey, ownerPermissionKeys, blockingStubFull);

    Long balanceAfter = PublicMethedForDailybuild.queryAccount(ownerAddress, blockingStubFull)
        .getBalance();
    logger.info("balanceAfter: " + balanceAfter);
    Assert.assertEquals(balanceBefore - balanceAfter, needCoin);

    PublicMethedForDailybuild
        .unFreezeBalance(fromAddress, testKey002, 0, ownerAddress, blockingStubFull);
  }

  @Test(enabled = true, description = "Witness permission_name length is 32")
  public void testWitnessName09() {
    String ownerKey = witnessKey001;
    byte[] ownerAddress = new WalletClient(ownerKey).getAddress();

    long needCoin = updateAccountPermissionFee * 2;

    Assert.assertTrue(PublicMethedForDailybuild.sendcoin(ownerAddress, needCoin + 1000000, fromAddress,
        testKey002, blockingStubFull));
    Assert.assertTrue(PublicMethedForDailybuild
        .freezeBalanceForReceiver(fromAddress, 100000000L, 0, 0,
            ByteString.copyFrom(ownerAddress),
            testKey002, blockingStubFull));
    PublicMethedForDailybuild.waitProduceNextBlock(blockingStubFull);
    PublicMethedForDailybuild.waitProduceNextBlock(blockingStubFull);
    Long balanceBefore = PublicMethedForDailybuild.queryAccount(ownerAddress, blockingStubFull)
        .getBalance();
    logger.info("balanceBefore: " + balanceBefore);

    List<String> ownerPermissionKeys = new ArrayList<>();

    PublicMethedForDailybuild.printAddress(ownerKey);
    PublicMethedForDailybuild.printAddress(tmpKey02);

    ownerPermissionKeys.add(ownerKey);

    String accountPermissionJson =
        "{\"owner_permission\":{\"type\":0,\"permission_name\":\"owner\",\"threshold\":1,\"keys\":["
            + "{\"address\":\"" + PublicMethedForDailybuild.getAddressString(testKey002)
            + "\",\"weight\":1}]},"
            + "\"witness_permission\":"
            + "{\"permission_name\":\"abcdefghijklmnopqrstuvwxyzabcdef\",\"type\":1,"
            + "\"threshold\":1,\"keys\":["
            + "{\"address\":\"" + PublicMethedForDailybuild.getAddressString(tmpKey02)
            + "\",\"weight\":1}]},"
            + "\"active_permissions\":[{\"type\":2,\"permission_name\":\"active\",\"threshold\":1,"
            + "\"operations\":\"3f3d1ec0032001000000000000000000000000000000000000000000000000c0\","
            + "\"keys\":["
            + "{\"address\":\"" + PublicMethedForDailybuild.getAddressString(witnessKey001) + "\",\"weight\":1},"
            + "{\"address\":\"" + PublicMethedForDailybuild.getAddressString(tmpKey02) + "\",\"weight\":1}"
            + "]}]}";

    Assert.assertTrue(PublicMethedForMutiSign.accountPermissionUpdate(accountPermissionJson,
        ownerAddress, ownerKey, blockingStubFull,
        ownerPermissionKeys.toArray(new String[ownerPermissionKeys.size()])));

    PublicMethedForDailybuild.waitProduceNextBlock(blockingStubFull);

    ownerPermissionKeys.clear();
    ownerPermissionKeys.add(testKey002);

    Assert.assertEquals(2, PublicMethedForMutiSign.getActivePermissionKeyCount(
        PublicMethedForDailybuild.queryAccount(ownerAddress, blockingStubFull).getActivePermissionList()));

    Assert.assertEquals(1, PublicMethedForDailybuild.queryAccount(ownerAddress,
        blockingStubFull).getOwnerPermission().getKeysCount());

    PublicMethedForMutiSign.printPermissionList(PublicMethedForDailybuild.queryAccount(ownerAddress,
        blockingStubFull).getActivePermissionList());

    System.out
        .printf(PublicMethedForMutiSign.printPermission(PublicMethedForDailybuild.queryAccount(ownerAddress,
            blockingStubFull).getOwnerPermission()));

    PublicMethedForMutiSign
        .recoverWitnessPermission(ownerKey, ownerPermissionKeys, blockingStubFull);

    PublicMethedForDailybuild.waitProduceNextBlock(blockingStubFull);

    Long balanceAfter = PublicMethedForDailybuild.queryAccount(ownerAddress, blockingStubFull)
        .getBalance();
    logger.info("balanceAfter: " + balanceAfter);

    Assert.assertEquals(balanceBefore - balanceAfter, needCoin);

    PublicMethedForDailybuild
        .unFreezeBalance(fromAddress, testKey002, 0, ownerAddress, blockingStubFull);

  }


  @Test(enabled = true, description = "Witness permission_name length is 33")
  public void testWitnessName10() {
    String ownerKey = witnessKey001;
    byte[] ownerAddress = new WalletClient(ownerKey).getAddress();

    Assert.assertTrue(PublicMethedForDailybuild.sendcoin(ownerAddress, 1000000, fromAddress,
        testKey002, blockingStubFull));

    PublicMethedForDailybuild.waitProduceNextBlock(blockingStubFull);
    PublicMethedForDailybuild.waitProduceNextBlock(blockingStubFull);
    Long balanceBefore = PublicMethedForDailybuild.queryAccount(ownerAddress, blockingStubFull)
        .getBalance();
    logger.info("balanceBefore: " + balanceBefore);

    List<String> ownerPermissionKeys = new ArrayList<>();

    PublicMethedForDailybuild.printAddress(ownerKey);
    PublicMethedForDailybuild.printAddress(tmpKey02);

    String accountPermissionJson =
        "{\"owner_permission\":{\"type\":0,\"permission_name\":\"owner\",\"threshold\":1,\"keys\":["
            + "{\"address\":\"" + PublicMethedForDailybuild.getAddressString(testKey002)
            + "\",\"weight\":1}]},"
            + "\"witness_permission\":"
            + "{\"permission_name\":\"abcdefghijklmnopqrstuvwxyzabcdefg\",\"type\":1,"
            + "\"threshold\":1,\"keys\":["
            + "{\"address\":\"" + PublicMethedForDailybuild.getAddressString(tmpKey02)
            + "\",\"weight\":1}]},"
            + "\"active_permissions\":[{\"type\":2,\"permission_name\":\"active\",\"threshold\":1,"
            + "\"operations\":\"3f3d1ec0032001000000000000000000000000000000000000000000000000c0\","
            + "\"keys\":["
            + "{\"address\":\"" + PublicMethedForDailybuild.getAddressString(witnessKey001) + "\",\"weight\":1},"
            + "{\"address\":\"" + PublicMethedForDailybuild.getAddressString(tmpKey02) + "\",\"weight\":1}"
            + "]}]}";

    GrpcAPI.Return response = PublicMethedForDailybuild.accountPermissionUpdateForResponse(
        accountPermissionJson, ownerAddress, ownerKey, blockingStubFull);

    Assert.assertFalse(response.getResult());
    Assert.assertEquals(CONTRACT_VALIDATE_ERROR, response.getCode());
    Assert.assertEquals("contract validate error : permission's name is too long",
        response.getMessage().toStringUtf8());

    Long balanceAfter = PublicMethedForDailybuild.queryAccount(ownerAddress, blockingStubFull)
        .getBalance();
    logger.info("balanceAfter: " + balanceAfter);

    Assert.assertEquals(balanceBefore, balanceAfter);

  }

  /**
   * constructor.
   */
  @AfterClass
  public void shutdown() throws InterruptedException {
    if (channelFull != null) {
      channelFull.shutdown().awaitTermination(5, TimeUnit.SECONDS);
    }
  }

}
