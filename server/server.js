var Hapi = require('hapi');

var server = new Hapi.Server();
server.connection({ port: 22258 });
var io = require("socket.io")(server.listener);

var heartbeat;
var clientCount = 0;
var cdo1State = {
  message: 'heartbeat',
  direction: 10,
  speed: 20,
  voltage: 30,
  lastUpdate: new Date(),
  connections: clientCount,
  driver: "marius"
};

function udpateClients() {
  cdo1State.lastUpdate = new Date();
  //console.info('broadcasting heartbeat ' + cdo1State.lastUpdate);
  if (heartbeat && cdo1State.connections <= 0) {
    clearInterval(heartbeat);
  }
  io.sockets.emit(
    'heartbeat',
    cdo1State
  );
}

var ioHandler = function (socket) {
  //console.log('a user connected');
  cdo1State.connections++;

  if (!heartbeat && 1==2) {
    heartbeat = setInterval(function() {
      udpateClients();
    }, 5000);
  };

  socket.emit("log-message", {
    message: "Hello from Hapi! " + Hapi.version
  });

  socket.on('disconnect', function(){
    cdo1State.connections--;
  });

  socket.on('echo', function(msg){
    socket.emit("echo", msg);
  });

  socket.on('control-message', function(msg){
    cdo1State.driver = msg.driver;
    cdo1State.direction = msg.direction;
    cdo1State.speed = msg.speed;
    //console.log('message: ' + cdo1State.direction);
    udpateClients();
  });
};

io.on("connection", ioHandler);


server.route({
    method: 'GET',
    path: '/',
    handler: function (request, reply) {
        reply('Hello, world!');
    }
});

server.route({
  path: "/{public*}",
  method: "GET",
  handler: {
    directory: {
      path: "./public"
    }
  }
})


server.start(function () {
    //console.log('Server running at:', server.info.uri);
});


