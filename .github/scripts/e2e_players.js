const mineflayer = require('mineflayer');
const { Vec3 } = require('vec3');

const [port, requestedVersion] = process.argv.slice(2);
// versions that share a protocol with the release minecraft-data names them after
const version = { '1.19.1': '1.19.2', '1.21.7': '1.21.8' }[requestedVersion] || requestedVersion;

if (!require('minecraft-data')(version)) {
  console.log(`E2E SKIP: the player library has no protocol data for ${requestedVersion}`);
  process.exit(2);
}
const failures = [];
let totalChecks = 0;

const sleep = (ms) => new Promise((resolve) => setTimeout(resolve, ms));

function check(name, ok, detail) {
  totalChecks++;
  console.log(`E2E ${ok ? 'PASS' : 'FAIL'}: ${name}${detail ? ` (${detail})` : ''}`);
  if (!ok) {
    failures.push(name);
  }
}

async function safeRun(name, fn) {
  console.log(`\n--- [Step] ${name} ---`);
  try {
    await fn();
  } catch (error) {
    check(name, false, error.message);
  }
}

async function waitFor(probe, timeout) {
  const end = Date.now() + timeout;
  while (Date.now() < end) {
    const value = probe();
    if (value) {
      return value;
    }
    await sleep(250);
  }
  return null;
}

function connect(username) {
  const bot = mineflayer.createBot({
    host: '127.0.0.1',
    port: Number(port),
    username,
    version,
    auth: 'offline',
    checkTimeoutInterval: 60000,
  });
  bot.log = [];
  bot.on('messagestr', (message) => bot.log.push(message));
  bot.on('error', (error) => console.log(`${username} error: ${error.message}`));
  bot.on('kicked', (reason) => console.log(`${username} kicked: ${JSON.stringify(reason)}`));
  return new Promise((resolve, reject) => {
    const timer = setTimeout(() => reject(new Error(`${username} did not spawn`)), 90000);
    bot.once('spawn', () => {
      clearTimeout(timer);
      resolve(bot);
    });
  });
}

async function openWindow(bot, block) {
  let opened = false;
  const onOpen = () => {
    opened = true;
  };
  bot.once('windowOpen', onOpen);
  await bot.activateBlock(block).catch(() => {});
  await waitFor(() => opened, 4000);
  bot.removeListener('windowOpen', onOpen);
  if (opened && bot.currentWindow) {
    bot.closeWindow(bot.currentWindow);
    await sleep(300);
  }
  return opened;
}

async function giveAndPlace(bot, itemName, offsetVec) {
  bot.chat(`/give ${bot.username} ${itemName} 1`);
  const item = await waitFor(() => bot.inventory.items().find((i) => i.name === itemName), 8000);
  if (!item) {
    throw new Error(`${bot.username} did not receive ${itemName}`);
  }
  const ref = bot.blockAt(bot.entity.position.floored().offset(offsetVec.x, offsetVec.y, offsetVec.z));
  await bot.equip(item, 'hand');
  await bot.placeBlock(ref, new Vec3(0, 1, 0));
  await sleep(1500);
  const placedPos = ref.position.offset(0, 1, 0);
  const block = bot.blockAt(placedPos);
  if (!block || block.name !== itemName) {
    throw new Error(`failed to place ${itemName}`);
  }
  return placedPos;
}

async function main() {
  const owner = await connect('BPOwner');
  await sleep(3000);
  owner.chat('/gamemode creative');
  await sleep(1000);
  owner.chat('/fill ~-4 ~-1 ~-4 ~4 ~-1 ~4 stone');
  owner.chat('/fill ~-4 ~ ~-4 ~4 ~3 ~4 air');
  await sleep(1500);

  let chestPosition = null;
  let barrelPosition = null;
  let doorPosition = null;
  let trapdoorPosition = null;

  await safeRun('owner chest placement', async () => {
    chestPosition = await giveAndPlace(owner, 'chest', { x: 1, y: -1, z: 0 });
    check('owner places a chest', owner.blockAt(chestPosition).name === 'chest');
    const notice = owner.log.find((line) => /protected/i.test(line));
    console.log(`placement notice: ${notice || 'none captured'}`);
    check('owner opens the protected chest', await openWindow(owner, owner.blockAt(chestPosition)));
  });

  await safeRun('/bp command check', async () => {
    owner.chat('/bp');
    const answered = await waitFor(() => owner.currentWindow || owner.log.some((line) => /blockprot|bp /i.test(line)), 6000);
    check('/bp answers the owner', Boolean(answered));
    if (owner.currentWindow) {
      owner.closeWindow(owner.currentWindow);
      await sleep(300);
    }
  });

  await safeRun('owner barrel placement', async () => {
    barrelPosition = await giveAndPlace(owner, 'barrel', { x: -1, y: -1, z: 0 });
    check('owner places a barrel', owner.blockAt(barrelPosition).name === 'barrel');
    check('owner opens the protected barrel', await openWindow(owner, owner.blockAt(barrelPosition)));
  });

  await safeRun('owner door placement', async () => {
    doorPosition = await giveAndPlace(owner, 'oak_door', { x: 0, y: -1, z: 2 });
    check('owner places a door', owner.blockAt(doorPosition)?.name === 'oak_door');
  });

  await safeRun('owner trapdoor placement', async () => {
    trapdoorPosition = await giveAndPlace(owner, 'oak_trapdoor', { x: 0, y: -1, z: -2 });
    check('owner places a trapdoor', owner.blockAt(trapdoorPosition)?.name === 'oak_trapdoor');
  });

  const intruder = await connect('BPIntruder');
  await sleep(2000);
  owner.chat('/tp BPIntruder BPOwner');
  await sleep(2500);

  if (chestPosition) {
    await safeRun('intruder chest protection', async () => {
      const target = intruder.blockAt(chestPosition);
      check('intruder sees the chest', Boolean(target) && target.name === 'chest');
      check('intruder cannot open the protected chest', !(await openWindow(intruder, target)));
      await Promise.race([intruder.dig(target).catch(() => {}), sleep(15000)]);
      await sleep(1500);
      check('intruder cannot break the protected chest', owner.blockAt(chestPosition).name === 'chest');
    });
  }

  if (barrelPosition) {
    await safeRun('intruder barrel protection', async () => {
      const target = intruder.blockAt(barrelPosition);
      check('intruder sees the barrel', Boolean(target) && target.name === 'barrel');
      check('intruder cannot open the protected barrel', !(await openWindow(intruder, target)));
      await Promise.race([intruder.dig(target).catch(() => {}), sleep(15000)]);
      await sleep(1500);
      check('intruder cannot break the protected barrel', owner.blockAt(barrelPosition).name === 'barrel');
    });
  }

  if (doorPosition) {
    await safeRun('intruder door protection', async () => {
      const target = intruder.blockAt(doorPosition);
      check('intruder sees the door', Boolean(target) && target.name === 'oak_door');
      await Promise.race([intruder.dig(target).catch(() => {}), sleep(15000)]);
      await sleep(1500);
      check('intruder cannot break the protected door', owner.blockAt(doorPosition)?.name === 'oak_door');
    });
  }

  if (trapdoorPosition) {
    await safeRun('intruder trapdoor protection', async () => {
      const target = intruder.blockAt(trapdoorPosition);
      check('intruder sees the trapdoor', Boolean(target) && target.name === 'oak_trapdoor');
      await Promise.race([intruder.dig(target).catch(() => {}), sleep(15000)]);
      await sleep(1500);
      check('intruder cannot break the protected trapdoor', owner.blockAt(trapdoorPosition)?.name === 'oak_trapdoor');
    });
  }

  await safeRun('item frame protection', async () => {
    owner.chat('/give BPOwner stone 1');
    const stoneItem = await waitFor(() => owner.inventory.items().find((i) => i.name === 'stone'), 5000);
    if (!stoneItem) return;
    const wallRef = owner.blockAt(owner.entity.position.floored().offset(2, -1, 1));
    await owner.equip(stoneItem, 'hand');
    await owner.placeBlock(wallRef, new Vec3(0, 1, 0));
    await sleep(1000);

    owner.chat('/give BPOwner item_frame 1');
    const frameItem = await waitFor(() => owner.inventory.items().find((i) => i.name === 'item_frame'), 5000);
    if (!frameItem) return;
    await owner.equip(frameItem, 'hand');
    const stoneBlock = owner.blockAt(wallRef.position.offset(0, 1, 0));
    await owner.activateBlock(stoneBlock, new Vec3(0, 0, 1)).catch(() => {});
    await sleep(1500);

    const frame = Object.values(owner.entities).find((e) => e.name === 'item_frame' || e.name === 'glow_item_frame');
    if (frame) {
      check('owner places an item frame', true);
      const intruderFrame = Object.values(intruder.entities).find((e) => e.id === frame.id);
      if (intruderFrame) {
        // mineflayer attack() is synchronous and returns undefined
        intruder.attack(intruderFrame);
        await sleep(1500);
        const stillPresent = Object.values(owner.entities).find((e) => e.id === frame.id);
        check('intruder cannot break the protected item frame', Boolean(stillPresent));
      }
    }
  });

  await safeRun('friends functionality', async () => {
    owner.chat('/bp friends addall BPIntruder');
    await sleep(2500);
    if (chestPosition) {
      const chestTarget = intruder.blockAt(chestPosition);
      check('friend can open the chest', await openWindow(intruder, chestTarget));
    }
    if (barrelPosition) {
      const barrelTarget = intruder.blockAt(barrelPosition);
      check('friend can open the barrel', await openWindow(intruder, barrelTarget));
    }
  });

  await safeRun('ownership transfer', async () => {
    owner.chat('/bp transferall BPIntruder');
    await sleep(2500);
    if (chestPosition) {
      const chestTarget = intruder.blockAt(chestPosition);
      check('new owner can open the chest after transfer', await openWindow(intruder, chestTarget));
    }
    if (barrelPosition) {
      const barrelTarget = intruder.blockAt(barrelPosition);
      check('new owner can open the barrel after transfer', await openWindow(intruder, barrelTarget));
    }
  });

  owner.quit();
  intruder.quit();
  await sleep(500);
}

main()
  .catch((error) => {
    console.log(`E2E FAIL: ${error.message}`);
    failures.push(error.message);
  })
  .finally(() => {
    const passed = totalChecks - failures.length;
    console.log('\n============================================================');
    console.log(`  Simulated Players Summary: ${passed}/${totalChecks} passed (${failures.length} failed)`);
    if (failures.length > 0) {
      console.log('  Failures:');
      for (const f of failures) {
        console.log(`    - ${f}`);
      }
    }
    console.log('============================================================\n');
    process.exit(failures.length ? 1 : 0);
  });
