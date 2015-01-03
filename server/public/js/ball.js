function Ball (radius, color) {
  if (radius === undefined) { radius = 40; }
  if (color === undefined) { color = "#ff0000"; }
  this.x = 0;
  this.y = 0;
  this.radius = radius;
  this.vx = 0;
  this.vy = 0;
  this.rotation = 0;
  this.scaleX = 1;
  this.scaleY = 1;
  this.color = utils.parseColor(color);
  this.lineWidth = 1;
}

Ball.prototype.draw = function (context) {

  var ambientLight = .1;
  var intensity = 1;
  var amb = 'rgba(0,0,0,' + (1-ambientLight) + ')';

  context.save();
  context.globalCompositeOperation = 'xor';

  context.translate(this.x, this.y);
  context.rotate(this.rotation);
  context.scale(this.scaleX, this.scaleY);

  /*
  context.lineWidth = this.lineWidth;
 */

  var g = context.createRadialGradient(0, 0, 0, 0, 0, this.radius);
  g.addColorStop(1, 'rgba(0,0,0,' + (1-intensity) + ')');
  g.addColorStop(0, amb);
  context.fillStyle = g;
  context.beginPath();
  context.arc(0, 0, this.radius, 0, Math.PI*2, true);
  context.closePath();
  context.fill();



/*
  context.fillStyle = this.color;
  context.beginPath();
  //x, y, radius, start_angle, end_angle, anti-clockwise
  context.arc(-1*this.radius/2, 0, this.radius/4, 0, (Math.PI * 2), true);
  context.arc(this.radius/2, 0, this.radius/4, 0, (Math.PI * 2), true);
  context.closePath();
  context.fill();
  if (this.lineWidth > 0) {
    context.stroke();
  }
  */
  context.restore();

};

Ball.prototype.getBounds = function () {
  return {
    x: this.x - this.radius,
    y: this.y - this.radius,
    width: this.radius * 2,
    height: this.radius * 2
  };
};
